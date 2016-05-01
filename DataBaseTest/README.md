本文主要对比基于 Android SQLiteDatabase 引擎实现的数据库框架：

greenDAO，官网链接  http://greenrobot.org/greendao 。

LiteOrm，官网链接 [http://litesuits.com](http://litesuits.com?from=jianshu_dbvs) 。

写本文机缘起于微信群里有人谈到Android数据库框架，随之一个有赞的朋友 @段扬扬 给了一份自己的测试数据，大概是这样：

![Android 数据库框架性能测试](http://upload-images.jianshu.io/upload_images/1459496-4f77e91e37b734f5.jpg?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

由这个图可以看到 Realm(https://realm.io/cn)  基本上是性能最好的，它确实是一个牛逼的项目，它不是基于 SQLite 而是基于一个自己的持久化引擎，具有 DB 文件跨平台共享的优点，也存在一些不大不小的问题，[点击这里查看](http://www.jcodecraeer.com/a/anzhuokaifa/androidkaifa/2015/1203/3743.html)（若链接失效请google关键词“为什么我不再使用Realm”），综合而言，还是可以一战，有兴趣可以自己尝试下。

里面没有 LiteOrm（和 Ormlite 不是一回事）的测试数据，大概是知名度小，我就随后要了一份他的测试代码和原始数据（特别感谢这位同学），不过这份数据不全了，而且需要同一部测试机，所以暂没法做框架间全量对比了。

所以，本文主要针对 greenDAO 和 LiteOrm，因为据说 greenDAO 是基于Android SQLite的最快、性能最强悍的数据库框架，因为他不涉及反射，靠的是代码辅助生成。

那么，我们从几个简单常见的角度和案例出发看看两者的表现如何，将会涉及到：

1. 性能表现情况
2. 初步使用情况
3. 应对需求变化
4. 待续...(精力有限，等有机缘在弄噢)

### 一. LiteOrm 和 greenDAO 的性能表现


下面是一组直观的测试数据，分为循环操作和批量操作两种场景：

![greenDAO vs LiteOrm 循环测试](http://upload-images.jianshu.io/upload_images/1459496-97fec0afbc3a43c6.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

![greenDAO vs LiteOrm 批量测试](http://upload-images.jianshu.io/upload_images/1459496-e6650e4395eb247c.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

**测试相关过程：**
1. 运行 Test Demo，点击 LiteOrm 测试按钮，通过日志观察执行完毕。

2. 命令行卸载 Test Demo，重新运行，点击 GreenDAO 测试按钮，通过日志观察执行完毕。

3. 每次点击按钮，所有操作会连续测试 10 次，取 10 次消耗时间的均值，安静等待结果就好了。

**测试相关信息：**
1. 测试机为 Nexus5，取 10 次消耗时间的均值。

2. 为了更直观清晰的观察数据，将循环操作和批量操作分开统计，否则因为两者数据差异过大，柱状图无法看清小数据的数值。

2. 循环单个操作比较耗时，每次操作 1000 条数据。

3. 批量操作因为整体是事务的，效率非常高，每次操作 100000 条数据。

**测试相关结论：**
1. [循环插入]、[循环更新] 以及 [批量更新] 时，LiteOrm性能略强于greenDAO。

2. [批量插入]、[查询操作] 时，LiteOrm性能略逊于greenDAO。

3. 除了 [批量查询] 以外，其他性能优劣势差距不明显，[批量查询]耗时差异主要来源于 LiteOrm 采用反射创建实例并赋值属性，而 greenDAO 使用 new 操作直接创建对象并直接赋值属性。

### 二. LiteOrm 和 greenDAO 的用法对比

我们以Note对象为例，展示操作过程，Note类如下：

```java
public class Note {
  private Long id;
  private String text;
  private String comment;
  private java.util.Date date;

  public Note() {}

  public Note(Long id, String text, String comment, java.util.Date date) {
        this.id = id;
        this.text = text;
        this.comment = comment;
        this.date = date;
    }

  // getter and setter...
}
```

实例化它：

```java
Note note = new Note(null, "title", "comment", new Date());
```

##### 1. greenDAO 增改查删
---

1.1 New Module：即新建一个子项目模块，选择 Java Libray，它是一个java 项目，用来生成 greenDAO 所需要的辅助代码。

1.2 写DAO生成器：即子模块里写一个 DAOGenerator 类生成  DAO、Master、Session 等对象：

```java
public class NoteDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, "lite.dbtest.greendao");

        addNote(schema);

        new DaoGenerator().generateAll(schema, "./app/src/main/java");
    }

    /** 指定 表名 和 列名，以及主键 */
    private static void addNote(Schema schema) {
        Entity note = schema.addEntity("Note");     // 默认表名为类名
        note.setTableName("CustomNote");            // 自定义表名
        note.addIdProperty().primaryKey().autoincrement(); //设置自增的主键
        note.addStringProperty("text").notNull(); // 非空字段
        note.addStringProperty("comment");
        note.addDateProperty("date");
    }
}
```

1.3 开始工作：实例化 DAO 对象后执行各种操作：

```java
 // 实例化 DAO
DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "greendao-notes", null);
SQLiteDatabase db = helper.getWritableDatabase();
DaoMaster daoMaster = new DaoMaster(db);
DaoSession daoSession = daoMaster.newSession();
NoteDao noteDao = daoSession.getNoteDao();

// 执行插入
noteDao.insert(note);
// 执行更新
noteDao.update(note);
// 执行查询
noteDao.queryBuilder().where(NoteDao.Properties.Id.eq(1)).list();
// 执行删除
noteDao.delete(note);
```

##### 2. LiteOrm 增改查删
---

2.1 开始工作：实例化 LiteOrm 对象后执行插入操作：

```java
// 实例化 LiteOrm
LiteOrm liteOrm = LiteOrm.newSingleInstance(this, "liteorm-notes");

// 执行插入
liteOrm.insert(note);
// 执行更新
liteOrm.update(note);
// 执行查询
liteOrm.queryById(1, Note.class);
// 执行删除
liteOrm.delete(note);
```

2.2 不要沉迷，没有第二步，操作已经完了。。。
简单解释下：上面例子默认类名为表名，字段名为列名，id(或者_id)属性为主键，但若要自定义 表名、列名 和 主键，需要给Model加注解：

```java
// table name is "lite-note"
@Table("lite-note")
public class Note {

    @Column("_id")
    @PrimaryKey(AssignType.AUTO_INCREMENT)
    private Long id; // column name is "_id"

    @NotNull
    @Column("_text")
    private String text;// column name is "_text"

    private String comment;// column name is "comment"
    private java.util.Date date;// column name is "date"
}
```

### 二. LiteOrm 和 greenDAO 面对需求或模型更改

举个简单例子，Note对象增加了一系列新字段，假设新增一个[title] 的属性：

```java
public class Note {
    private Long id;
    private String title; // 新增这个 title 字段
    private String text;
    private String comment;
    private java.util.Date date;

    // 其他省略
}
```

#####  greenDAO 面对需求or对象模型更改
---

**第1步** 自定义 SQLiteOpenHelper

greenDAO 常见得做法是在自定义你使用的 SQLiteOpenHelper，比如下面方案。

方案a ： 删旧表，建新表。
```java
    /** WARNING: Drops all table on Upgrade! Use only during development. */
    public static class DevOpenHelper extends OpenHelper {
        public DevOpenHelper(Context context, String name, CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 方案1，删旧表，建新表。
            dropAllTables(db, true);
            onCreate(db);
        }
    }
```
方案b ：为旧表添加新列。
```java
    /** WARNING: Drops all table on Upgrade! Use only during development. */
    public static class DevOpenHelper extends OpenHelper {
        public DevOpenHelper(Context context, String name, CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // 方案2，为旧表添加新列。
            db.execSQL("ALTER TABLE NOTE ADD COLUMN title");
        }
    }
```

如果不做上面操作，那么升级后，是会发生下面这个异常，因为之前建的表不存在[title]这个列呀！

```java
android.database.sqlite.SQLiteException: no such table: CustomNote (code 1): , while compiling: INSERT INTO "CustomNote" ("_id","TEXT","COMMENT","TITLE","DATE") VALUES (?,?,?,?,?)                                                         
```

**第2步**  修改 DAOGenerator 重新生成代码

因为 greenDAO 操作的模型是由其代码生成工具产生的，需要在 DAOGenerator 里添加一个字段，让其重新生成一次模型。

```java
 Schema schema = new Schema(2, "lite.dbtest.greendao"); // 升级数据库版本

Entity note = schema.addEntity("Note"); 
note.setTableName("CustomNote");
note.addIdProperty().primaryKey().autoincrement();
note.addStringProperty("title"); // 这里新增一个字段
note.addStringProperty("text").notNull();
note.addStringProperty("comment");
note.addDateProperty("date");

 new DaoGenerator().generateAll(schema, "./app/src/main/java");
```
运行，greenDAO 通过 Schema 设置了数据库版本，为我们生成了系列新的Note、Note DAO、Master、Session等类。

至此，然后基本完成 [添加一个属性字段] 的升级改造。

#####  LiteOrm 面对需求or对象模型更改
---
好害怕，会不会更麻烦。。。but。。。
事实是 1 步也不需要走，什么都不用改，因为模型里面我们已经新增了一个字段：

```java
public class Note {
    private Long id;
    private String title; // 新增这个 title 字段
    // ... 其他省略
```
这已经足够了，这受益于 LiteOrm 的自动探测技术，它会智能的判断某个对象是不是发生了改变，从而同步到数据库，这一切，开发者是无感知的。

限于时间和个人精力问题，这篇分析并不全面，如果有误还请不吝指正。不论哪款 ORM 或 数据库框架，都各有利弊，至于该选用哪一款，可自行斟酌，开发者最好自己亲身体验下，毕竟绝知此事需躬行，只听或者看别人的言论和结果，无异于直接吃别人嚼过的东西，没有味道不重要，变了味会影响个人判断。

测试代码上传到这里：https://github.com/litesuits/for-test/tree/master/DataBaseTest