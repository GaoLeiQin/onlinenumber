import com.ledo.common.Task;
import org.junit.Test;

/**
 * 定时任务测试
 * @author qgl
 * @date 2018/11/13
 */
public class TaskTest {

    @Test
    public void test() {
    }

    @Test
    public void sqlTest() {
        Task task = new Task(4);
        task.startScheduleRateTask(new SaveServerInfoTask(5));
        task.startScheduleDelayTask(new SaveUrlContentTask(8));
    }

    public class SaveServerInfoTask implements Runnable {
        private int count;

        public SaveServerInfoTask(int count) {
            this.count = count;
        }

        @Override
        public void run() {
            System.out.println("保存服务器信息！" + count);
        }
    }

    public class SaveUrlContentTask implements Runnable {
        private int count;

        public SaveUrlContentTask(int count) {
            this.count = count;
        }

        @Override
        public void run() {
            System.out.println("存储网页内容：" + count);
        }
    }
}