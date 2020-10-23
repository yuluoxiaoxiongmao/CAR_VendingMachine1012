package com.example.vendingmachine.utils;

/**
 * Created by Tony on 2020-05-18.
 * <p>
 * ArraySplit.java
 * Copyright(C) 2014
 * creator:cuiran 2014-8-4 上午10:39:28
 */

/**
 * ArraySplit.java
 * Copyright(C) 2014
 * creator:cuiran 2014-8-4 上午10:39:28
 */

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author cuiran
 * @version 1.0.0
 */
public class ArraySplit {

    /**
     * 2014-8-4 上午10:39:28
     *
     */
    public static void main() {
        // TODO Auto-generated method stub

        int[] ary = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};//要分割的数组
        byte[] key_1 = new byte[8];
        key_1[0] = (byte) 0x3D;
        key_1[1] = (byte) 0x75;
        key_1[2] = (byte) 0x95;
        key_1[3] = (byte) 0xA9;
        key_1[4] = (byte) 0x8B;
        key_1[5] = (byte) 0xFF;
        key_1[6] = (byte) 0x80;
        key_1[7] = (byte) 0x9D;
        int splitSize = 8;//分割的块大小
//        List list = Arrays.asList(arr);
        Object[] subAry = splitAry(ary, splitSize);//分割后的子块数组

        for (Object obj : subAry) {//打印输出结果
            int[] aryItem = (int[]) obj;
            for (int i = 0; i < aryItem.length; i++) {
                System.out.print(aryItem[i] + ", ");
                Log.e("array_test",aryItem[i]+",");
            }
            System.out.println();
            Log.e("array_test","=================================");
        }

    }

    /**
     * splitAry方法<br>
     * 2014-8-4 上午10:45:36
     * @param ary 要分割的数组
     * @param subSize 分割的块大小
     * @return
     *
     */
    private static Object[] splitAry(int[] ary, int subSize) {
        int count = ary.length % subSize == 0 ? ary.length / subSize : ary.length / subSize + 1;

        List<List<Integer>> subAryList = new ArrayList<List<Integer>>();

        for (int i = 0; i < count; i++) {
            int index = i * subSize;
            List<Integer> list = new ArrayList<Integer>();
            int j = 0;
            while (j < subSize && index < ary.length) {
                list.add(ary[index++]);
                j++;
            }
            subAryList.add(list);
        }

        Object[] subAry = new Object[subAryList.size()];

        for (int i = 0; i < subAryList.size(); i++) {
            List<Integer> subList = subAryList.get(i);
            int[] subAryItem = new int[subList.size()];
            for (int j = 0; j < subList.size(); j++) {
                subAryItem[j] = subList.get(j).intValue();
            }
            subAry[i] = subAryItem;
        }

        return subAry;
    }
}
