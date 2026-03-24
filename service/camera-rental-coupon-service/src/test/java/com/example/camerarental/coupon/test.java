package com.example.camerarental.coupon;

import java.util.Scanner;

public class test {
    public static void main(String[] args) {
        // 1. 修正数组长度：33组对应数组长度33，索引0-32
        int[] sum = new int[34];
        int count = 0;
        int x = 0;
        // 2. 只创建一次Scanner，避免输入流混乱
        Scanner sc = new Scanner(System.in);

        // 3. 外层循环：34组
        for (int i = 0; i < 34; i++) {
            // 4. 每组求和前重置count为0，避免累加前一组结果
            count = 0;
            // 内层循环：每组5个数字
            for (int j = 0; j < 5; j++) {
                int a = sc.nextInt();
                count += a;
            }
            // 5. 后置自增：先赋值给sum[x]，再x++，从sum[0]开始填充
            sum[x++] = count;
        }

        // 关闭Scanner
        sc.close();

        // 输出每组求和结果（每行一个）
        System.out.println("===== 每组5个数字求和结果 =====");
        for (int i = 0; i < 34; i++) {
            System.out.println(sum[i]);

        }
    }
}
