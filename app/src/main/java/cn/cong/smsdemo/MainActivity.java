package cn.cong.smsdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.TextView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private TextView tv;

    private String phone = "1600123412341234";
    private ContentObserver smsObserver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv = findViewById(R.id.tv);


        // 系统作为提供者，咱们只需要解析内容。。内容解析者(增删改查)-最适合操作数据库
        // 短信、联系人、通讯记录、音乐、相册、视频


        // 通过uri找到目标：短信的内容提供者
        // uri:1、百度 2、看源码：http://androidxref.com/
        Uri uri = Uri.parse("content://sms");
        // observer观察者
        smsObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);

                Log.d("SmsDemo", uri.getScheme() + " " + uri.getAuthority() + " " + uri.getPath());

                Cursor cs = getContentResolver().query(uri, new String[]{"address", "body"}, null, null, "date desc");

                if (cs.moveToNext()) {
                    String address = cs.getString(0);
                    String body = cs.getString(1);
                    if (phone.equals(address)) {
                        // 提取验证码
                        Pattern pattern = Pattern.compile("[0-9]{4,6}");
                        Matcher matcher = pattern.matcher(body);
                        if (matcher.find()) {
                            String code = matcher.group();
                            // 显示到界面上
                            tv.setText(code);
                        }
                    }
                }

            }
        };
        getContentResolver().registerContentObserver(uri, true, smsObserver);



    }


    // 动态注册-短信的广播接受者
    private class SmsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
                // 接收到短信
                Bundle bundle = intent.getExtras();
                // 如果一条短信过长，可能会分割成几条短信发送，所有需要用多个pdu代表一个短信(72中文，140字符)
//                byte[][] pdus = (byte[][]) bundle.get("pdus");// byte[] pbu
                Object[] pdus = (Object[]) bundle.get("pdus");// byte[] pbu

                // 短信内容
                StringBuilder sb = new StringBuilder(); // 字符串拼接（不牵扯线程）

                for (int i = 0; i < pdus.length; i++) {
                    SmsMessage message = SmsMessage.createFromPdu((byte[]) pdus[i]);
                    // 1、判断一下是不是我们发的
                    String address = message.getDisplayOriginatingAddress();
                    if (phone.equals(address)) {
                        // 是我们发的，把这个pdu的内容拼接进去
                        // 2、从中提取短信内容
                        sb.append(message.getDisplayMessageBody());
                    }
                }
                // 3、从内容中提取验证码 // sb: [xxx公司]您正在申请XX业务，您的验证码是：123456，有效时间为~~~~。
                Pattern pattern = Pattern.compile("[0-9]{4,6}");
                Matcher matcher = pattern.matcher(sb);
                if (matcher.find()) {
                    String code = matcher.group();
                    // 显示到界面上
                    tv.setText(code);
                }
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解除注册
        getContentResolver().unregisterContentObserver(smsObserver);
    }
}
