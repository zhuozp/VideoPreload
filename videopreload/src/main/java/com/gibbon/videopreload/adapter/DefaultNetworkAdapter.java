package com.gibbon.videopreload.adapter;

/**
 * @author zhipeng.zhuo
 * @date 2020-06-18
 */
public class DefaultNetworkAdapter implements INetworkAdapter {
    @Override
    public boolean canPreLoadIfNotWifi() {
        return false;
    }
}
