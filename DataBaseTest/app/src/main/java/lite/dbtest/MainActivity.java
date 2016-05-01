package lite.dbtest;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import lite.dbtest.greendao.DaoMaster;
import lite.dbtest.greendao.DaoSession;
import lite.dbtest.greendao.NoteDao;
import lite.dbtest.model.Note;
import com.litesuits.orm.LiteOrm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private static NoteDao noteDao;
    private static LiteOrm liteOrm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init lite-orm
        if (liteOrm == null) {
            liteOrm = LiteOrm.newSingleInstance(this, "liteorm-notes");
        }

        // init GreenDAO
        if (noteDao == null) {
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "greendao-notes", null);
            SQLiteDatabase db = helper.getWritableDatabase();
            DaoMaster daoMaster = new DaoMaster(db);
            DaoSession daoSession = daoMaster.newSession();
            noteDao = daoSession.getNoteDao();
        }

    }

    private int testTimes = 1; // 整体测试次数，取其均值。
    private int batchCount = 10000; // 10万条数据 测试批量
    private int singleCount = 100; // 1千条数据 测试循环单个操作

    public void clickGreenDAO(View view) {
        new Thread() {
            @Override
            public void run() {
                TimeCounter timeCounter = new TimeCounter();

                for (int i = 0; i < testTimes; i++) {
                    testGreenDAOBatch(timeCounter, batchCount);
                    testGreenDAOSingle(timeCounter, singleCount);
                }

                Log.i(TAG, "Test GreenDAO " + testTimes + " times, Average Time : " + timeCounter.toString());
            }
        }.start();
    }

    public void clickLiteOrm(View view) {
        new Thread() {
            @Override
            public void run() {
                TimeCounter timeCounter = new TimeCounter();

                for (int i = 0; i < testTimes; i++) {
                    testLiteOrmBatch(timeCounter, batchCount);
                    testLiteOrmSingle(timeCounter, singleCount);
                }

                Log.i(TAG, "Test LiteOrm " + testTimes + " times, Average Time : " + timeCounter.toString());
            }
        }.start();
    }


    private void testGreenDAOBatch(final TimeCounter timeCounter, final int count) {
        long affectedRows, time;

        final ArrayList<Note> noteList = new ArrayList<Note>();

        // init data
        for (int i = 0; i < count; i++) {
            Note note = new Note(null, "title", "comment", new Date());
            noteList.add(note);
        }

        Log.i(TAG, "greendao test begin... just wait for result. ");

        // 1. 批量插入
        time = System.currentTimeMillis();
        noteDao.insertInTx((Note[]) noteList.toArray(new Note[noteList.size()]));
        time = System.currentTimeMillis() - time;
        timeCounter.insertBatchList.add(time);
        Log.i(TAG, "greedDAO —— insert —— batch " + noteList.size() + " data, use time " + time + " ms");


        // 2. 批量更新
        for (Note note : noteList) {
            note.setText("up_batch_title");
            note.setComment("up_batch_comment");
        }
        time = System.currentTimeMillis();
        noteDao.updateInTx(noteList);
        time = System.currentTimeMillis() - time;
        timeCounter.updateBatchList.add(time);
        Log.d(TAG, "greedDAO —— update —— batch  " + count + " data, use time " + time + " ms");

        // 3. 批量查询
        time = System.currentTimeMillis();
        List<Note> list2 = noteDao.queryBuilder().list();
        time = System.currentTimeMillis() - time;
        timeCounter.queryBatchList.add(time);
        Log.i(TAG, "greedDAO —— query —— batch  " + list2.size() + " data, use time " + time + " ms");
        Log.i(TAG, "greedDAO batch query data list" + list2);


        // 4. 删除全部
        time = System.currentTimeMillis();
        noteDao.deleteAll();
        time = System.currentTimeMillis() - time;
        timeCounter.deleteAllList.add(time);
        Log.d(TAG, "greedDAO —— delete —— all  " + count + " data, use time " + time + " ms");

        // 5. 查询确认是否全部删除
        List<Note> list3 = noteDao.queryBuilder().list();
        Log.i(TAG, "greedDAO left data size --------> " + list3.size());
    }

    private void testGreenDAOSingle(final TimeCounter timeCounter, final int count) {
        long affectedRows, time;

        final ArrayList<Note> noteList = new ArrayList<Note>();

        // init data
        for (int i = 0; i < count; i++) {
            Note note = new Note(null, "title", "comment", new Date());
            noteList.add(note);
        }

        // 1. 循环插入，每次插入一条数据
        time = System.currentTimeMillis();
        for (Note note : noteList) {
            noteDao.insert(note);
        }
        time = System.currentTimeMillis() - time;
        timeCounter.insertSingleList.add(time);
        Log.i(TAG,
                "greedDAO —— insert —— one-by-one  " + noteList.size() + " data, use time " + time + " ms");

        // 2. 循环更新，每次更新一条数据
        time = System.currentTimeMillis();
        for (Note note : noteList) {
            note.setText("update_title");
            note.setComment("update_comment");
            noteDao.update(note);
        }
        time = System.currentTimeMillis() - time;
        timeCounter.updateSingleList.add(time);
        Log.d(TAG, "greedDAO —— update —— one-by-one  " + count + " data, use time " + time + " ms");

        // 3. 循环查询，每次查询一条数据
        time = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            noteDao.queryBuilder().where(NoteDao.Properties.Id.eq(1)).list();
        }
        time = System.currentTimeMillis() - time;
        timeCounter.querySingleList.add(time);
        Log.i(TAG, "greedDAO —— query —— one-by-one  " + count + " data, use time " + time + " ms");


        // 4. 删除
        time = System.currentTimeMillis();
        noteDao.deleteAll();
        time = System.currentTimeMillis() - time;
        timeCounter.deleteAllList.add(time);
        Log.d(TAG, "greedDAO —— delete —— all  " + count + " data, use time " + time + " ms");

        // 5. 查询确认是否全部删除
        List<Note> list3 = noteDao.queryBuilder().list();
        Log.i(TAG, "greedDAO left data size --------> " + list3.size());
    }

    private void testLiteOrmBatch(TimeCounter timeCounter, int count) {
        long affectedRows, time;

        final ArrayList<Note> noteList = new ArrayList<Note>();

        // init data
        for (int i = 0; i < count; i++) {
            Note note = new Note(null, "title", "comment", new Date());
            noteList.add(note);
        }

        Log.i(TAG, "lite-orm test begin... just wait for result. ");

        // 1. 批量插入
        time = System.currentTimeMillis();
        affectedRows = liteOrm.insert(noteList);
        time = System.currentTimeMillis() - time;
        timeCounter.insertBatchList.add(time);
        Log.i(TAG, "lite-orm —— insert —— batch " + affectedRows + " data, use time " + time + " ms");

        // 2. 批量更新
        for (Note note : noteList) {
            note.setText("up_batch_title");
            note.setComment("up_batch_comment");
        }
        time = System.currentTimeMillis();
        affectedRows = liteOrm.update(noteList);
        time = System.currentTimeMillis() - time;
        timeCounter.updateBatchList.add(time);
        Log.d(TAG, "lite-orm —— update —— batch " + affectedRows + " data, use time " + time + " ms");

        // 3. 批量查询数据
        time = System.currentTimeMillis();
        List<Note> list2 = liteOrm.query(Note.class);
        time = System.currentTimeMillis() - time;
        timeCounter.queryBatchList.add(time);
        Log.i(TAG, "lite-orm —— query —— batch " + list2.size() + " data, use time " + time + " ms");
        Log.i(TAG, "lite-orm —— query —— batch " + list2);


        // 4. 删除数据
        time = System.currentTimeMillis();
        liteOrm.deleteAll(Note.class);
        time = System.currentTimeMillis() - time;
        timeCounter.deleteAllList.add(time);
        Log.d(TAG, "lite-orm —— delete —— all " + count + " data, use time " + time + " ms");

        // 5. 查询确认是否全部删除
        List<Note> list3 = liteOrm.query(Note.class);
        Log.i(TAG, "lite-orm left data size --------> " + list3.size());
    }

    private void testLiteOrmSingle(TimeCounter timeCounter, int count) {
        long affectedRows, time;

        final ArrayList<Note> noteList = new ArrayList<Note>();

        // init data
        for (int i = 0; i < count; i++) {
            Note note = new Note(null, "title", "comment", new Date());
            noteList.add(note);
        }

        Log.i(TAG, "lite-orm test begin... just wait for result. ");

        // 1. 循环插入，每次插入一条数据
        time = System.currentTimeMillis();
        for (Note note : noteList) {
            liteOrm.insert(note);
        }
        time = System.currentTimeMillis() - time;
        timeCounter.insertSingleList.add(time);
        Log.i(TAG,
                "lite-orm —— insert —— one-by-one " + noteList.size() + " data, use time " + time + " ms");


        // 2. 循环更新，每次更新一条数据
        time = System.currentTimeMillis();
        for (Note note : noteList) {
            note.setText("update_title");
            note.setComment("update_comment");
            liteOrm.update(note);
        }
        time = System.currentTimeMillis() - time;
        timeCounter.updateSingleList.add(time);
        Log.d(TAG, "lite-orm —— update —— one-by-one " + count + " data, use time " + time + " ms");

        // 3. 循环查询数据，每次查询一条数据
        time = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            liteOrm.queryById(1, Note.class);
        }
        time = System.currentTimeMillis() - time;
        timeCounter.querySingleList.add(time);
        Log.i(TAG, "lite-orm —— query —— one-by-one " + count + " data, use time " + time + " ms");

        // 4. 删除数据
        time = System.currentTimeMillis();
        liteOrm.deleteAll(Note.class);
        time = System.currentTimeMillis() - time;
        timeCounter.deleteAllList.add(time);
        Log.d(TAG, "lite-orm —— delete —— all " + count + " data, use time " + time + " ms");

        // 5. 查询确认是否全部删除
        List<Note> list3 = liteOrm.query(Note.class);
        Log.i(TAG, "lite-orm left data size --------> " + list3.size());
    }
}
