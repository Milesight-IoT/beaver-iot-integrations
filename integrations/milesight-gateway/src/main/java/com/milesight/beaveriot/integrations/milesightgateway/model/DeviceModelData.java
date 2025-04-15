package com.milesight.beaveriot.integrations.milesightgateway.model;

import lombok.Data;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;

/**
 * DeviceModelData class.
 *
 * Store in deviceModelData of integration entity.
 *
 * @author simon
 * @date 2025/2/28
 */
@Data
public class DeviceModelData {
    String version;

    String source;

    List<VendorInfo> vendorInfoList;

    @Data
    public static class VendorInfo {
        private String id;

        private String name;

        private String devices;

        List<DeviceInfo> deviceInfoList;
    }

    @Data
    public static class DeviceInfo {
        private String id;

        private String name;
    }

    public static String getDeviceModelId(VendorInfo vendor, DeviceInfo device) {
        return getDeviceModelId(new VendorDeviceInfo(vendor, device));
    }

    public static String getDeviceModelId(VendorDeviceInfo vendorDeviceInfo) {
        return vendorDeviceInfo.getDeviceId() + "@" + vendorDeviceInfo.getVendorId();
    }

    public static String getDeviceModelName(VendorInfo vendor, DeviceInfo device) {
        return device.getName() + " (" + vendor.getName() + ")";
    }

    @Data
    public static class VendorDeviceInfo {
        private String deviceId;

        private String deviceName;

        private String vendorId;

        private String vendorName;

        private String vendorDevices;

        public VendorDeviceInfo() {}

        public VendorDeviceInfo(VendorInfo vendorInfo, DeviceInfo deviceInfo) {
            this.setDeviceId(deviceInfo.getId());
            this.setDeviceName(deviceInfo.getName());
            this.setVendorId(vendorInfo.getId());
            this.setVendorName(vendorInfo.getName());
            this.setVendorDevices(vendorInfo.getDevices());
        }
    }

    public VendorDeviceInfo findByVendorDeviceId(String id) {
        AtomicReference<VendorDeviceInfo> result = new AtomicReference<>();
        iterateWhen((vendorInfo, deviceInfo) -> {
            if (getDeviceModelId(vendorInfo, deviceInfo).equals(id)) {
                result.set(new VendorDeviceInfo(vendorInfo, deviceInfo));
                return false;
            }

            return true;
        });

        return result.get();
    }

    public void iterateWhen(BiPredicate<VendorInfo, DeviceInfo> function) {
        for (VendorInfo vendorInfo : this.getVendorInfoList()) {
            for (DeviceInfo deviceInfo : vendorInfo.getDeviceInfoList()) {
                if (!function.test(vendorInfo, deviceInfo)) {
                    return;
                }
            }
        }
    }
}
