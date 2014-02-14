package log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Owner
 * Date: 2/10/14
 * Time: 4:54 PM
 * To change this template use File | Settings | File Templates.
 */
public final class LogManager {
    public static List<Log> logs = new ArrayList<Log>();

    public static void registerLog(Log l) {
        logs.add(l);
    }

    public static void cleanup() {
        for(Log l : logs) {
            l.close();
        }
    }
}
