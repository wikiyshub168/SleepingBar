package com.bolaa.sleepingbar.watch;

import android.app.Notification;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.bolaa.sleepingbar.model.Watch;
import com.bolaa.sleepingbar.utils.AppUtil;
import com.bolaa.sleepingbar.utils.DateUtil;
import com.core.framework.develop.LogUtil;
import com.core.framework.store.sharePer.PreferencesUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 保持手环与app的数据交互，处理数据逻辑
 * 直接通过设备的address连接
 * 这里不进行扫描设备,只尝试连接
 * 同时只能连接一支手环,如果传入的address不同，那么进行连接
 * Created by paulz on 2016/5/27.
 */
public class WatchService extends Service{
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    public static final String FLAG_CURRENT_DEVICE_ADDRESS="flag_current_device_address";
    public static final String FLAG_CURRENT_DEVICE_NAME="flag_current_device_name";


    /**搜索BLE终端*/
    private BluetoothAdapter mBluetoothAdapter;
    /**读写BLE终端*/
    private BluetoothLeClass mBLE;
    private boolean mScanning;
    private Handler mHandler=new Handler();
    private String currentAddress;
    private BluetoothGattCharacteristic writeCharacteristic;
    private BluetoothGattCharacteristic readCharacteristic;

    WatchCMDReceiver mReceiver;

    boolean isNeedAdjustingTime=false;



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        registBroadcast();
        startCheck();
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        scanLeDevice(true);
        String newAddress=null;
        if(intent!=null){
            newAddress =intent.getStringExtra(FLAG_CURRENT_DEVICE_ADDRESS);
        }
        if(newAddress==null){
            newAddress = PreferencesUtils.getString(FLAG_CURRENT_DEVICE_ADDRESS);
        }
        if(!AppUtil.isNull(currentAddress)&&!AppUtil.isNull(newAddress)&&!currentAddress.equals(intent.getStringExtra(FLAG_CURRENT_DEVICE_ADDRESS))){
            mBLE.close();
        }
        currentAddress=newAddress;
        tryConnect(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregistBroadcast();
        mBLE.close();
        stopCheck();
        //断开连接时，首页步行数据清空
        sendBroadcast(new Intent(WatchConstant.ACTION_WATCH_DISCONNECTED));
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        LogUtil.d("watch---onLowMemory-->执行");
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        LogUtil.d("watch---onTrimMemory-->执行 level="+level);
    }

    private void registBroadcast(){
        if(mReceiver!=null)return;
        mReceiver=new WatchCMDReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction(WatchConstant.ACTION_WATCH_CMD_SET_DATE);
        filter.addAction(WatchConstant.ACTION_WATCH_CMD_SET_INFO);
        filter.addAction(WatchConstant.ACTION_WATCH_CMD_GET_SLEEP);
        registerReceiver(mReceiver,filter);
    }

    private void unregistBroadcast(){
        if(mReceiver!=null){
            unregisterReceiver(mReceiver);
            mReceiver=null;
        }
    }

    private void init() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            LogUtil.d("watch----没有4.0蓝牙权限");
            stopSelf();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            LogUtil.d("watch----获取蓝牙适配器失败");
            stopSelf();
            return;
        }
        //直接开启蓝牙
        mBluetoothAdapter.enable();
        //初始化蓝牙工具类
        mBLE = new BluetoothLeClass(this);
        if (!mBLE.initialize()) {
            LogUtil.d("watch-----Unable to initialize Bluetooth");
            stopSelf();
            return;
        }
        //发现BLE终端的Service时回调
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        //收到BLE终端数据交互的事件
        mBLE.setOnDataAvailableListener(mOnDataAvailable);

        mBLE.setOnConnectListener(new BluetoothLeClass.OnConnectListener() {
            @Override
            public void onConnect(BluetoothGatt gatt) {
                LogUtil.d("watch---onConnect---device="+gatt.getDevice()+"("+gatt.getDevice().getName()+") ,connected Devices="+bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size());
            }
        });

        mBLE.setOnDisconnectListener(new BluetoothLeClass.OnDisconnectListener() {
            @Override
            public void onDisconnect(BluetoothGatt gatt) {
                LogUtil.d("watch---onDisconnect---device="+gatt.getDevice()+"("+gatt.getDevice().getName()+") ,connected Devices="+bluetoothManager.getConnectedDevices(BluetoothProfile.GATT).size());
            }
        });
    }

    private void tryConnect(Intent intent){
        if(TextUtils.isEmpty(currentAddress)){
            LogUtil.d("watch---无效mac地址,关闭服务...");
            stopSelf();
            return;
        }
        boolean isSuc=mBLE.connect(currentAddress);
        LogUtil.d("watch---尝试连接:"+isSuc);
        if(!isSuc){
            //尝试连接失败，直接关闭
            stopSelf();
        }
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(new UUID[]{WatchConstant.UUID_SERVICE},mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * 搜索到BLE终端服务的事件
     */
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new BluetoothLeClass.OnServiceDiscoverListener(){

        @Override
        public void onServiceDiscover(final BluetoothGatt gatt) {
            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    enableNotification(gatt,true);
//                    enableNotificationWrite(gatt);
                }
            });
            //读取手环mac地址---暂时不需要
           /* new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    CMDHandler.cmdGetMacAddress(writeCharacteristic);
                    mBLE.writeCharacteristic(writeCharacteristic);
                }
            },5*1000);*/

        }

    };

    /**
     * 收到BLE终端数据交互的事件
     */
    private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new BluetoothLeClass.OnDataAvailableListener(){

        /**
         * BLE终端数据被读的事件
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                LogUtil.d("watch---onCharRead "+gatt.getDevice().getName()
                        +" read "
                        +characteristic.getUuid().toString()
                        +" -> "
                        +Utils.bytesToHexString(characteristic.getValue()));
            LogUtil.d("watch---onCharRead "+gatt.getDevice().getName()
                    +" read "
                    +characteristic.getUuid().toString()
                    +" -> "
                    + Arrays.toString(characteristic.getValue()));
        }

        /**
         * 收到BLE终端写入数据回调
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic) {
            LogUtil.d("watch---onCharWrite "+gatt.getDevice().getName()
                    +" write "
                    +characteristic.getUuid().toString()
                    +" -> "
                    +Arrays.toString(characteristic.getValue()));
        }

//        int[][] data0x17=new int[192][16];
//        int i=0;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //收到手环的上报消息
            LogUtil.d("watch---onCharNotify----"+gatt.getDevice().getName()+"----notify--"+characteristic.getUuid().toString()+"--orig-->"+Arrays.toString(characteristic.getValue())+"--cmd(0x"+Utils.bytesToHexString(new byte[]{characteristic.getValue()[0]})+")");
//            LogUtil.d("watch---onCharNotify----"+gatt.getDevice().getName()+"----notify--"+characteristic.getUuid().toString()+"--hex-->"+Utils.bytesToHexString(characteristic.getValue())+"--cmd(0x"+Utils.bytesToHexString(new byte[]{characteristic.getValue()[0]})+")");
            LogUtil.d("watch---onCharNotify----"+gatt.getDevice().getName()+"----notify--"+characteristic.getUuid().toString()+"--dest-->"+Arrays.toString(Utils.bytesToIntArrayV2(characteristic.getValue()))+"--end-");

//            byte[] data=characteristic.getValue();
//            int[] ints=new int[16];
//            if(data[0]==17){
//                ints[0]=((data[4]<<24)&0xff000000)|((data[3]<<16)&0x00ff0000)|((data[2]<<8)&0x0000ff00)|((data[1]&0x000000ff));
//                for(int j=5;j<data.length;j++){
//                    ints[j-4]=data[j];
//                }
//                data0x17[i++]=ints;
//            }else if(data[0]==16){
//                ints[0]=((data[4]<<24)&0xff000000)|((data[3]<<16)&0x00ff0000)|((data[2]<<8)&0x0000ff00)|((data[1]&0x000000ff));
//                data0x17[i++]=ints;
//            }

            CMDHandler.synchronizedMovement(WatchService.this,characteristic.getValue());
            //读取最近两天的数据
            int resultCode=CMDHandler.saveSleep(characteristic.getValue());
            if(resultCode==1){
                //昨天的
                CMDHandler.cmdGetSleepInfo(writeCharacteristic,(byte)1);
                mBLE.writeCharacteristic(writeCharacteristic);
            }else if(resultCode==2){
//                i=0;
//                new Thread(){
//                    @Override
//                    public void run() {
//                        for(int x=0;x<data0x17.length;x++){
//                            LogUtil.d("watch----all i="+x+"--"+Arrays.toString(data0x17[x]));
//                        }
//                    }
//                }.start();
                //获取完毕
                PreferencesUtils.putString(WatchConstant.FLAG_SLEEP_DATA_FOR_MAC,gatt.getDevice().getAddress());
                startService(new Intent(WatchService.this,WatchUploadService.class).setAction(WatchUploadService.ACTION_WATCH_UPLOAD_SERVICE)
                        .putExtra(WatchConstant.FLAG_ONCE_UPLOAD_SLEEP_DATA_TODAY,PreferencesUtils.getString("sleep_data_today"))
                        .putExtra(WatchConstant.FLAG_ONCE_UPLOAD_SLEEP_DATA_YESTERDAY,PreferencesUtils.getString("sleep_data_yesterday"))
                        .putExtra(WatchConstant.FLAG_ONCE_UPLOAD_SLEEP_DATA_DATE,PreferencesUtils.getString("sleep_data_collect_date"))
                        .putExtra(WatchConstant.FLAG_ONCE_UPLOAD_SLEEP_DATA_MAC,gatt.getDevice().getAddress())
                );
                //再次写入时间,防止之前没写进去
                if(isNeedAdjustingTime){
                    CMDHandler.cmdSetDate(writeCharacteristic,(int)(System.currentTimeMillis()/1000+(8*60*60)));
                    mBLE.writeCharacteristic(writeCharacteristic);
                    isNeedAdjustingTime=false;
                }
            }
        }
    };

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (device != null&&device.getName().contains("aceband")){
                                if (mScanning) {
                                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                    mScanning = false;
                                }
                                mBLE.connect(device.getAddress());
                            }
                        }
                    });
                }
            };

    //必须在主线程执行
    public void enableNotification(BluetoothGatt mBluetoothGatt,boolean b){
            BluetoothGattService service =mBluetoothGatt.getService(UUID.fromString("000056ff-0000-1000-8000-00805f9b34fb"));
            if(service==null){
                AppUtil.showToast(getApplicationContext(),"请连接小趣手环");
                return;
            }
            readCharacteristic =service.getCharacteristic(WatchConstant.UUID_CHARA_READ);
            writeCharacteristic =service.getCharacteristic(WatchConstant.UUID_CHARA_WRITE);

            boolean set = mBluetoothGatt.setCharacteristicNotification(readCharacteristic, true);

            LogUtil.d("watch--- setnotification = " + set);
            BluetoothGattDescriptor dsc =readCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            if(dsc==null){
                AppUtil.showToast(getApplicationContext(),"该设备无可用特征通道");
                return;
            }
            new Handler(getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    CMDHandler.cmdGetSleepInfo(writeCharacteristic,(byte)0);
                    mBLE.writeCharacteristic(writeCharacteristic);
                }
            },3*1000);
            dsc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean success =mBluetoothGatt.writeDescriptor(dsc);
            LogUtil.d("watch---writing enabledescriptor:" + success);
//            Toast.makeText(getApplicationContext(),"通知开起:"+set+"--写入:"+success,Toast.LENGTH_LONG).show();
            if(set&&success){
                LogUtil.d("watch---connect---success---mac="+mBluetoothGatt.getDevice().getAddress());
                sendBroadcast(new Intent(WatchConstant.ACTION_WATCH_CONNECTED_SUCCESS).putExtra("device_name",mBluetoothGatt.getDevice().getName()).putExtra("device_address",mBluetoothGatt.getDevice().getAddress()));
            }
        }

    //必须在主线程执行
    public void enableNotificationWrite(BluetoothGatt mBluetoothGatt){

        boolean set = mBluetoothGatt.setCharacteristicNotification(writeCharacteristic, true);
        LogUtil.d("watch 33f3--- setnotification = " + set);
        if(AppUtil.isEmpty(writeCharacteristic.getDescriptors()))return;
        BluetoothGattDescriptor dsc =writeCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        dsc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean success =mBluetoothGatt.writeDescriptor(dsc);
        LogUtil.d("watch 33f3---writing enabledescriptor:" + success);
        Toast.makeText(getApplicationContext(),"33f3通知开起:"+set+"--写入:"+success,Toast.LENGTH_LONG).show();
    }


    public class WatchCMDReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();
            if(WatchConstant.ACTION_WATCH_CMD_SET_INFO.equals(action)){
                byte[] info=intent.getByteArrayExtra(WatchConstant.FLAG_USER_INFO);
                if(readCharacteristic!=null){
                    CMDHandler.cmdSetInfo(readCharacteristic,info[0],info[1],info[2],info[3]);
                    mBLE.writeCharacteristic(readCharacteristic);
                }
            }else if(WatchConstant.ACTION_WATCH_CMD_SET_DATE.equals(action)){
                if(intent.getIntExtra(WatchConstant.FLAG_DEVICE_DATE,0)>0){
                    isNeedAdjustingTime=true;
                    CMDHandler.cmdSetDate(writeCharacteristic,(int)(System.currentTimeMillis()/1000+(8*60*60)));
                    mBLE.writeCharacteristic(writeCharacteristic);
                }
            }else if(WatchConstant.ACTION_WATCH_CMD_GET_SLEEP.equals(action)){//读手环的睡眠信息
                if(writeCharacteristic!=null){
                    CMDHandler.cmdGetSleepInfo(writeCharacteristic,(byte)0);
                    mBLE.writeCharacteristic(writeCharacteristic);
                }
            }
        }
    }

    private long mSwitchInterval=1000*60;


    private class CheckConnectionTask implements Runnable{

        @Override
        public void run() {
            if(!mBLE.isConnected()){
                boolean isSuc=mBLE.reconnect();
                LogUtil.d("watch reconnect isSuc="+isSuc);
            }
        }
    };

    private class checkTask implements Runnable{

        @Override
        public void run() {
            if(!mBLE.isConnected()){
                mHandler.post(new CheckConnectionTask());
            }
        }
    };
    private ScheduledExecutorService mScheduleService;

    public void startCheck() {
        stopCheck();
        mScheduleService = Executors.newSingleThreadScheduledExecutor();
        mScheduleService.scheduleAtFixedRate(new checkTask(),
                mSwitchInterval , mSwitchInterval, TimeUnit.MILLISECONDS);
    }


    public void stopCheck() {
        if (mScheduleService != null && !mScheduleService.isShutdown()) {
            mScheduleService.shutdownNow();
        }
    }

}
