package test;

import java.net.Inet4Address;
import java.net.UnknownHostException;

public class IpTest {
    public static void main(String[] args) throws UnknownHostException {
        String ip = Inet4Address.getLocalHost().getHostAddress();
        System.out.println(ip);
    }
}
