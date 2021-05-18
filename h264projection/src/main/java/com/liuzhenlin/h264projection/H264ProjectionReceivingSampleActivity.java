package com.liuzhenlin.h264projection;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;

import com.google.android.material.textfield.TextInputLayout;
import com.liuzhenlin.common.Consts;
import com.liuzhenlin.common.socket.SocketClient;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class H264ProjectionReceivingSampleActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Dialog dialog = new AppCompatDialog(this, R.style.DialogStyle_MinWidth_NoTitle);
        dialog.setContentView(View.inflate(this, R.layout.dialog_enter_ip_address, null));
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getWindow().getDecorView().setTag(dialog.findViewById(R.id.textinput_ipAddress));

        View cancelBtn = dialog.findViewById(R.id.btn_cancel_enterIpAddressDialog);
        cancelBtn.setTag(dialog);
        cancelBtn.setOnClickListener(this);

        View okBtn = dialog.findViewById(R.id.btn_ok_enterIpAddressDialog);
        okBtn.setTag(dialog);
        okBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_cancel_enterIpAddressDialog) {
            ((Dialog) v.getTag()).dismiss();
            finish();
        } else if (id == R.id.btn_ok_enterIpAddressDialog) {
            Dialog dialog = (Dialog) v.getTag();
            TextInputLayout editor = (TextInputLayout) dialog.getWindow().getDecorView().getTag();
            String ipAddress = editor.getEditText().getText().toString().trim();
            if (ipAddress.matches(Consts.REGEX_IP_ADDRESS)) {
                setContentView(ipAddress);
                dialog.dismiss();
            } else {
                Toast.makeText(this, R.string.illegalIpAddress, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setContentView(String ipAddress) {
        setContentView(R.layout.activity_h264_projection_receiving_sample);
        SurfaceView surfaceView = findViewById(R.id.surfaceView);
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            SocketClient client;
            H264ProjectionReceiver player;

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                player = new H264ProjectionReceiver();
                player.start(holder.getSurface());
                client = new SocketClient(H264ProjectionSendingSampleActivity.SOCKET_PORT);
                client.setCallback(player);
                client.connect(ipAddress);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                client.disconnect();
                client = null;
                player.stop();
                player = null;
            }
        });
    }
}
