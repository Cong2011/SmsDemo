package cn.cong.smsdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// 用广播接受者，接收短信（备份）
public class MainActivity_Receiver extends AppCompatActivity {

    private Button bt;
    private SmsReceiver smsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bt = findViewById(R.id.bt);

        smsReceiver = new SmsReceiver();


        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        registerReceiver(smsReceiver, filter);

        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

    }


    // 动态注册-短信的广播接受者
    private class SmsReceiver extends BroadcastReceiver {

        private String phone = "1600123412341234";

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
                    bt.setText(code);
                }
            }
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsReceiver);
    }
}
