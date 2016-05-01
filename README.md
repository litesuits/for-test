
### 一. LiteOrm 和 greenDAO 的性能对比


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
