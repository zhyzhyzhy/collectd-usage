package org.zhy;

/**
 * Created by Administrator on 2017/3/27.
 */
import com.alibaba.fastjson.JSONReader;
import org.collectd.api.*;
import java.util.Date;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zhy on 1/21/17.
 */
public class Main implements CollectdConfigInterface, CollectdReadInterface {

    private static Long lastMod = 0l;
    private static String Path = "/data/task_status.json";
    private static ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

    private JSONReader reader;
    private File file;


    public Main() {
        Collectd.registerConfig("Main", this);
        Collectd.registerRead("Main", this);
    }

    public int config(OConfigItem oConfigItem) {
        return 0;
    }


    @Override
    public int read() {
        file = new File(Path);
        if (lastMod == 0l) {
            lastMod = file.lastModified();
            scanFile();
        } else if (lastMod != file.lastModified()) {
            lastMod = file.lastModified();
            scanFile();
        }
        return 0;
    }

    public void scanFile() {

        Notification notification = new Notification();
        notification.setMessage("");
        boolean flag = false;
        try {
            reader = new JSONReader(new FileReader(Path));

            reader.startObject();

            while (reader.hasNext()) {
                String mirrorName = reader.readString();
                if (map.get(mirrorName) == null) {
                    map.put(mirrorName, "");
                }

                reader.startObject();
                String message = "";
                Integer exitcode = 0;
                String date = "";
                while (reader.hasNext()) {

                    String string = reader.readString();
                    Object object = reader.readObject();

                    if (string.equals("message")) {
                        message = (String) object;
                    }

                    if (string.equals("date")) {
                        date = (String) object;
                    }

                    if (string.equals("exitcode")) {
                        exitcode = (Integer) object;
                    }
                }

                String lastDate = map.get(mirrorName);
                if (lastDate.equals("")) {
                    map.put(mirrorName,date);
                    if (exitcode != 0) {
                        flag = true;
                        notification.setTime(new Date().getTime());
                        notification.setSeverity(1);
                        notification.setPlugin("sync warning");
                        notification.setHost("localhost");
                        notification.setMessage(notification.getMessage() + " " + mirrorName + " : " + message);
                    }
                }
                else if (!date.equals(lastDate)) {
                    map.put(mirrorName, date);
                    if (exitcode != 0) {
                        flag = true;
                        notification.setTime(new Date().getTime());
                        notification.setSeverity(1);
                        notification.setPlugin("sync warning");
                        notification.setHost("localhost");
                        notification.setMessage(notification.getMessage() + " " + mirrorName + " : " + message);
                    }
                }
            }
            reader.endObject();
        reader.endObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            reader.close();
        }
        if (flag == true) {
            Collectd.logWarning("has send");
            Collectd.dispatchNotification(notification);
        }
    }

}
