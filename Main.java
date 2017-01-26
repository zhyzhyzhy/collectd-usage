import com.alibaba.fastjson.JSONReader;
import org.collectd.api.*;
import java.util.Date;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by zhy on 1/21/17.
 */
public class Main implements CollectdConfigInterface, CollectdReadInterface
{

    private JSONReader reader;
    private static Long lastMod = 0l;
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
        file = new File("/data/task_status.json");
        if(lastMod == 0l) {
            lastMod = file.lastModified();
            scanFile();
        }
        else if(lastMod != file.lastModified()) {
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
            reader = new JSONReader(new FileReader("/data/task_status.json"));

            reader.startObject();

            while (reader.hasNext()) {
                String mirrorName = reader.readString();

                reader.startObject();
                while (reader.hasNext()) {

                    String string = reader.readString();
                    Object object = reader.readObject();
                    if (string.equals("message")) {
                        String message = (String) object;
                        if (!message.equals("Sync succeed")) {
                            flag = true;
notification.setTime(new Date().getTime());
                            notification.setSeverity(1);
                            notification.setPlugin("sync warning");
                            notification.setHost("localhost");
                            notification.setMessage(notification.getMessage()+" "+mirrorName+" : " + message);

                        }
                    }
                }
                reader.endObject();
            }
            reader.endObject();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            reader.close();
        }
        if(flag == true) {
            Collectd.logWarning("has send");
            Collectd.dispatchNotification(notification);
        }
    }

}
