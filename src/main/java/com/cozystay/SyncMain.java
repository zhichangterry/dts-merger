package com.cozystay;

import com.cozystay.datasource.BinLogDataSourceImpl;
import com.cozystay.datasource.DTSDataSourceImpl;
import com.cozystay.datasource.DataSource;
import com.cozystay.model.SyncOperation;
import com.cozystay.model.SyncTask;
import com.cozystay.model.SyncTaskBuilder;
import com.cozystay.structure.ProcessedTaskPool;
import com.cozystay.structure.SimpleProcessedTaskPool;
import com.cozystay.structure.SimpleTaskRunnerImpl;
import com.cozystay.structure.TaskRunner;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SyncMain {
    @SuppressWarnings("FieldCanBeLocal")
    private static int MAX_DATABASE_SIZE = 10;

    public static void main(String[] args) throws Exception {
        System.out.print("DB Sync runner launched");
        Properties prop = new Properties();
        prop.load(SyncMain.class.getResourceAsStream("/db-config.properties"));
        Integer threadNumber = Integer.valueOf(prop.getProperty("threadNumber", "5"));
        System.out.printf("Running with %d threads%n",threadNumber);


        final List<DataSource> dataSources = new ArrayList<>();
        final ProcessedTaskPool pool = new SimpleProcessedTaskPool();


        final TaskRunner runner = new SimpleTaskRunnerImpl(1, threadNumber) {

            @Override
            public void addTask(SyncTask newRecord) {
                synchronized (pool) {
                    if (!pool.hasTask(newRecord)) {
                        pool.add(newRecord);
                        return;
                    }
                    SyncTask currentTask = pool.get(newRecord.getId());
                    SyncTask mergedTask = currentTask.merge(newRecord);
                    if (mergedTask.allOperationsCompleted()) {
                        pool.remove(currentTask);
                    } else {
                        pool.add(mergedTask);
                    }
                }

            }

            @Override
            public void workOn() {
                SyncTask toProcess;
                synchronized (pool) {
                    toProcess = pool.poll();
                    if (toProcess == null) {
//                        System.out.println("No SyncTask to process");
                        return;
                    }


                    for (DataSource source :
                            dataSources) {
                        for (SyncOperation operation : toProcess.getOperations()) {
                            if (operation.shouldSendToSource(source.getName())) {
                                source.writeDB(operation);
                                operation.setSourceSend(source.getName());
                            }
                        }
                    }
                    pool.add(toProcess);
                }
            }
        };
        runner.start();


        for (int i = 1; i <= MAX_DATABASE_SIZE; i++) {
            try {

                final DataSource source = new BinLogDataSourceImpl(prop, "db" + i) {
                    @Override
                    public void consumeData(SyncTask task) {
                        runner.addTask(task);

                    }

                };

                source.start();
                dataSources.add(source);
                SyncTaskBuilder.addSource(source.getName());


            } catch (ParseException e) {
                System.out.printf("Could not find DBConsumer%d, Running with %d consumers%n", i, i - 1);

                break;
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

        Thread.sleep(1000 * 60 * 24);



    }
}
