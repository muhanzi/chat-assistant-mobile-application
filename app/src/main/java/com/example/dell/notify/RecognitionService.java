package com.example.dell.notify;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class RecognitionService extends Service {
    public RecognitionService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
