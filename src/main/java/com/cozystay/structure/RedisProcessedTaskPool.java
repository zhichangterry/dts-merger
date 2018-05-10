package com.cozystay.structure;

import com.cozystay.model.SyncOperationImpl;
import com.cozystay.model.SyncTask;
import com.cozystay.model.SyncTaskImpl;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Set;

public class RedisProcessedTaskPool implements ProcessedTaskPool {
    private final String DATA_HASH_KEY = "cozy-data-hash";
    private final String DATA_SET_KEY = "cozy-data-sort-set";

    private Jedis redisClient;
    private Kryo kryo;


    public RedisProcessedTaskPool(String host, int port, String password) {
        JedisPool jedisPool = new JedisPool(host, port);

        redisClient = jedisPool.getResource();
        if(!password.equals("")){
            redisClient.auth(password);

        }
        kryo = new Kryo();
        kryo.register(SyncTaskImpl.class);
        kryo.register(SyncOperationImpl.class);
    }

    @Override
    public void add(SyncTask task) {
        byte[] taskArray = encode(kryo, task);
        Transaction transaction = redisClient.multi();
        transaction.hset(DATA_HASH_KEY.getBytes(), task.getId().getBytes(), taskArray);
        transaction.zadd(DATA_SET_KEY, new Date().getTime(), task.getId());
        transaction.exec();

    }

    @Override
    public void remove(SyncTask task) {
        this.remove(task.getId());

    }

    @Override
    public void remove(String taskId) {
        Transaction transaction = redisClient.multi();
        transaction.hdel(DATA_HASH_KEY.getBytes(), taskId.getBytes());
        transaction.zrem(DATA_SET_KEY, taskId);
        transaction.exec();
    }

    @Override
    public boolean hasTask(SyncTask task) {
        return redisClient.hexists(DATA_HASH_KEY, task.getId());
    }

    @Override
    public synchronized SyncTask poll() {
        Set<String> keySet = redisClient.zrange(DATA_SET_KEY, 0, 0);
        if (keySet.isEmpty()) {
            return null;
        }
        String key = (String) keySet.toArray()[0];
        SyncTask task = get(key);
        remove(key);
        return task;

    }

    @Override
    public synchronized SyncTask get(String taskId) {
        byte[] taskBytes = redisClient.hget(DATA_HASH_KEY.getBytes(), taskId.getBytes());
        return decode(this.kryo, taskBytes);
    }

    @Override
    public void destroy() {
        if(redisClient!=null && redisClient.isConnected()){
            redisClient.disconnect();
        }
    }

    private static byte[] encode(Kryo kryo, Object obj) {
        ByteArrayOutputStream objStream = new ByteArrayOutputStream();
        Output objOutput = new Output(objStream);
        kryo.writeClassAndObject(objOutput, obj);
        objOutput.close();
        return objStream.toByteArray();
    }

    private static <T> T decode(Kryo kryo, byte[] bytes) {
        return (T) kryo.readClassAndObject(new Input(bytes));
    }



}