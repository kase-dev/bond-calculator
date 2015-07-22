package kz.kase.util.test;

import kz.kase.util.BondCalculator;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BondCalcTest {

    public static void main(String[] args) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");


        //NFBNb1
//        BondType type = BondType.COUPON;
//        BasisType basis = BasisType.TYPE_30_360;
//        Date clearDate = formatter.parse("2012-09-27");
//        Date lastPayDate = formatter.parse("2011-03-27");
//        Date nextPayDate = formatter.parse("2011-09-27");
//        Date stopDate = formatter.parse("2011-09-26");
//        double accruedInterest = 4.46083;
//        double couponTax = 10.1;
//        int payCnt = 2;

        //NTK091_1305
//        BondType type = BondType.DISCOUNT;
//        BasisType basis = BasisType.TYPE_ACTUAL_365;
//        Date clearDate = formatter.parse("2011-10-28");
//        Date lastPayDate = null;
//        Date nextPayDate = null;
//        Date stopDate = null;
//        double accruedInterest = 0.0;
//        double couponTax = 0.0;
//        int payCnt = 0;

        //NRBNb5
//        BondType type = BondType.COUPON;
//        BasisType basis = BasisType.TYPE_30_360;
//        Date clearDate = formatter.parse("2016-05-18");
//        Date lastPayDate = formatter.parse("2011-05-18");
//        Date nextPayDate = formatter.parse("2011-11-18");
//        Date stopDate = formatter.parse("2011-11-17");
//        double accruedInterest = 6.665093965544957;
//        double couponTax = 7.5;
//        int payCnt = 2;

        BondCalculator.BondType type = BondCalculator.BondType.COUPON;
        BondCalculator.BasisType basis = BondCalculator.BasisType.TYPE_30_360;
        Date clearDate = formatter.parse("2016-05-30");
        Date lastPayDate = formatter.parse("2011-05-30");
        Date nextPayDate = formatter.parse("2011-11-30");
        Date stopDate = formatter.parse("2011-11-29");
        double accruedInterest = 2.86;
        double couponTax = 10;
        int payCnt = 2;

        BondCalculator calculator = new BondCalculator(type, basis,
                clearDate, lastPayDate, nextPayDate, stopDate,
                accruedInterest, couponTax, payCnt);

//        double price = 105.0;
//        double interest = calculator.calcBondYield(price);
//        System.out.println(price + ":\t" + interest);

        for (int i = 0; i < 200; i++) {
            double price = 90 + 0.1 * i;
            double interest = calculator.calcBondYield(price).getValue();
            System.out.println(price + ":\t" + interest);
        }

//        for (int i = -100; i < 100; i++) {
////            double interest = i * 0.01;
//            double val = calculator.calculatePrice(i);
//            System.out.println(i + ";" + val);
//        }
    }
}
