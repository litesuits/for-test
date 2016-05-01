package lite.dbtest;

import java.util.ArrayList;

/**
 * @author 氢一 @http://def.so
 * @date 2016-04-29
 */
public class TimeCounter {
    public ArrayList<Long> insertBatchList = new ArrayList<>();
    public ArrayList<Long> updateBatchList = new ArrayList<>();
    public ArrayList<Long> queryBatchList = new ArrayList<>();

    public ArrayList<Long> insertSingleList = new ArrayList<>();
    public ArrayList<Long> updateSingleList = new ArrayList<>();
    public ArrayList<Long> querySingleList = new ArrayList<>();

    public ArrayList<Long> deleteAllList = new ArrayList<>();

    public long getTimeAverage(ArrayList<Long> timeList) {
        if (timeList == null || timeList.isEmpty()) {
            return 0;
        }
        long timeSum = 0;
        for (long time : timeList) {
            timeSum += time;
        }
        return timeSum / timeList.size();
    }

    @Override
    public String toString() {
        return "insertBatch=" + getTimeAverage(insertBatchList) +
               ", updateBatch=" + getTimeAverage(updateBatchList) +
               ", queryBatch=" + getTimeAverage(queryBatchList) +
               ", insertCycle=" + getTimeAverage(insertSingleList) +
               ", updateCycle=" + getTimeAverage(updateSingleList) +
               ", queryCycle=" + getTimeAverage(querySingleList) +
               ", deleteAll=" + getTimeAverage(deleteAllList);
    }
}
