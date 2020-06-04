package com.kumuluz.ee.common.utils;

public enum PackagingType {
    UBER,
    SKIMMED,
    EXPLODED;

    public static PackagingType getTypeFromString(String string){
        if (string == null){
            return null;
        }
        switch (string.trim().toLowerCase()){
            case "uber":
                return UBER;
            case "skimmed":
                return SKIMMED;
            case "exploded":
                return EXPLODED;
            default:
                return null;
        }
    }
}
