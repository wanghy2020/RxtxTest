package test;

import gnu.io.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * @description:
 * @author: Wang Hongyu
 * @date: 2020-10-15
 */
public class SerialPortTool {

    public static void main(String[] args) {

        // 查询串口
        findSystemAllComPort();

//        //发送16进制数据——实际应用中串口通信传输的数据，大都是 16 进制

        String hexStrCode = "455A432F5600";
        SerialPort serialPort = SerialPortTool.openComPort(null, 38400, 8, 1, 0);
        // 设置串口监听
        SerialPortTool.setListenerToSerialPort(serialPort, new SerialPortEventListener() {
            @Override
            public void serialEvent(SerialPortEvent arg0) {
                if (arg0.getEventType() == SerialPortEvent.DATA_AVAILABLE) {//数据通知
                    byte[] bytes = SerialPortTool.readData(serialPort);
                    System.out.println("收到的数据长度：" + bytes.length);
                    System.out.println("收到的数据：" + new String(bytes));
                }
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                int i = 1;
                while (i < 1000) {
                    byte[] bytes = hexStrCode.getBytes();
                    SerialPortTool.sendDataToComPort(serialPort, bytes);//发送数据
                    i++;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    /**
     * 查找电脑上所有可用 com 端口
     *
     * @return 可用端口名称列表，没有时 列表为空
     */
    public static final ArrayList<String> findSystemAllComPort() {
        /**
         *  getPortIdentifiers：获得电脑主板当前所有可用串口
         */
        Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
        ArrayList<String> portNameList = new ArrayList<>();

        /**
         *  将可用串口名添加到 List 列表
         */
        System.out.println("===========串口信息start==============");

        while (portList.hasMoreElements()) {
            CommPortIdentifier commPortIdentifier = portList.nextElement();
            StringBuffer returnStr = new StringBuffer();
            String portName = commPortIdentifier.getName();//名称如 COM1、COM2....
            String isOwner = String.valueOf(commPortIdentifier.isCurrentlyOwned());
            returnStr.append(isOwner).append("\t 占用情况:").append(isOwner);
            System.out.println(returnStr);
            portNameList.add(portName);
        }
        System.out.println("===========串口信息end==============");

        return portNameList;
    }

    /**
     * 打开电脑上指定的串口
     *
     * @param portName 端口名称，如 COM1，为 null 时，默认使用电脑中能用的端口中的第一个
     * @param b        波特率(baudrate)，如 9600
     * @param d        数据位（datebits），如 SerialPort.DATABITS_8 = 8
     * @param s        停止位（stopbits），如 SerialPort.STOPBITS_1 = 1
     * @param p        校验位 (parity)，如 SerialPort.PARITY_NONE = 0
     * @return 打开的串口对象，打开失败时，返回 null
     */
    public static final SerialPort openComPort(String portName, int b, int d, int s, int p) {
        CommPort commPort = null;
        try {
            //当没有传入可用的 com 口时，默认使用电脑中可用的 com 口中的第一个
            if (portName == null || "".equals(portName)) {
                List<String> comPortList = findSystemAllComPort();
                if (comPortList != null && comPortList.size() > 0) {
                    portName = comPortList.get(0);
                }
            }
            System.out.println("开始打开串口：portName=" + portName + ",baudrate=" + b + ",datebits=" + d + ",stopbits=" + s + ",parity=" + p);

            //通过端口名称识别指定 COM 端口
            CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
            /**
             * open(String TheOwner, int i)：打开端口
             * TheOwner 自定义一个端口名称，随便自定义即可
             * i：打开的端口的超时时间，单位毫秒，超时则抛出异常：PortInUseException if in use.
             * 如果此时串口已经被占用，则抛出异常：gnu.io.PortInUseException: Unknown Application
             */
            commPort = portIdentifier.open(portName, 5000);
            /**
             * 判断端口是不是串口
             * public abstract class SerialPort extends CommPort
             */
            if (commPort instanceof SerialPort) {
                SerialPort serialPort = (SerialPort) commPort;
                /**
                 * 设置串口参数：setSerialPortParams( int b, int d, int s, int p )
                 * b：波特率（baudrate）
                 * d：数据位（datebits），SerialPort 支持 5,6,7,8
                 * s：停止位（stopbits），SerialPort 支持 1,2,3
                 * p：校验位 (parity)，SerialPort 支持 0,1,2,3,4
                 * 如果参数设置错误，则抛出异常：gnu.io.UnsupportedCommOperationException: Invalid Parameter
                 * 此时必须关闭串口，否则下次 portIdentifier.open 时会打不开串口，因为已经被占用
                 */
                serialPort.setSerialPortParams(b, d, s, p);
                System.out.println("打开串口 " + portName + " 成功...");
                return serialPort;
            } else {
                System.out.println("当前端口 " + commPort.getName() + " 不是串口...");
            }
        } catch (NoSuchPortException e) {
            e.printStackTrace();
        } catch (PortInUseException e) {
            System.out.println("串口 " + portName + " 已经被占用，请先解除占用...");
            e.printStackTrace();
        } catch (UnsupportedCommOperationException e) {
            System.out.println("串口参数设置错误，关闭串口，数据位[5-8]、停止位[1-3]、验证位[0-4]...");
            e.printStackTrace();
            if (commPort != null) {//此时必须关闭串口，否则下次 portIdentifier.open 时会打不开串口，因为已经被占用
                commPort.close();
            }
        }
        System.out.println("打开串口 " + portName + " 失败...");
        return null;
    }

    /**
     * 往串口发送数据
     *
     * @param serialPort 串口对象
     * @param orders     待发送数据
     */
    public static void sendDataToComPort(SerialPort serialPort, byte[] orders) {
        OutputStream outputStream = null;
        try {
            if (serialPort != null) {
                outputStream = serialPort.getOutputStream();
                outputStream.write(orders);
                outputStream.flush();
                System.out.println("往串口 " + serialPort.getName() + " 发送数据：" + Arrays.toString(orders) + " 完成...");
            } else {
                System.out.println("gnu.io.SerialPort 为null，取消数据发送...");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从串口读取数据
     *
     * @param serialPort 要读取的串口
     * @return 读取的数据
     */
    public static byte[] readData(SerialPort serialPort) {
        InputStream is = null;
        byte[] bytes = null;
        try {
            is = serialPort.getInputStream();//获得串口的输入流
            int bufflenth = is.available();//获得数据长度
            while (bufflenth != 0) {
                bytes = new byte[bufflenth];//初始化byte数组
                is.read(bytes);
                bufflenth = is.available();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }

    /**
     * 给串口设置监听
     *
     * @param serialPort
     * @param listener
     */
    public static void setListenerToSerialPort(SerialPort serialPort, SerialPortEventListener listener) {
        try {
            //给串口添加事件监听
            serialPort.addEventListener(listener);
        } catch (TooManyListenersException e) {
            e.printStackTrace();
        }
        serialPort.notifyOnDataAvailable(true);//串口有数据监听
        serialPort.notifyOnBreakInterrupt(true);//中断事件监听
    }


    /**
     * 关闭串口
     *
     * @param serialPort 待关闭的串口对象
     */
    public static void closeComPort(SerialPort serialPort) {
        if (serialPort != null) {
            serialPort.close();
            System.out.println("关闭串口 " + serialPort.getName());
        }
    }

    /**
     * 16进制字符串转十进制字节数组
     * 这是常用的方法，如某些硬件的通信指令就是提供的16进制字符串，发送时需要转为字节数组再进行发送
     *
     * @param strSource 16进制字符串，如 "455A432F5600"，每两位对应字节数组中的一个10进制元素
     *                  默认会去除参数字符串中的空格，所以参数 "45 5A 43 2F 56 00" 也是可以的
     * @return 十进制字节数组, 如 [69, 90, 67, 47, 86, 0]
     */
    public static byte[] hexString2Bytes(String strSource) {
        if (strSource == null || "".equals(strSource.trim())) {
            System.out.println("hexString2Bytes 参数为空，放弃转换.");
            return null;
        }
        strSource = strSource.replace(" ", "");
        int l = strSource.length() / 2;
        byte[] ret = new byte[l];
        for (int i = 0; i < l; i++) {
            ret[i] = Integer.valueOf(strSource.substring(i * 2, i * 2 + 2), 16).byteValue();
        }
        return ret;
    }

}
