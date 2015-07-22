package kz.kase.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class BondCalculator {

    public static final long MILIS_IN_DAY = 1000 * 60 * 60 * 24;

    public enum BondType {
        DISCOUNT, COUPON
    }

    public enum BasisType {
        TYPE_ACTUAL_365(0),
        TYPE_30_360(1),
        TYPE_ACTUAL_360(2),
        TYPE_ACTUAL_364(3),
        TYPE_ACTUAL_182_183(4),
        TYPE_ACTUAL_ACTUAL(5);
        private int value = 0;

        BasisType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static BasisType getType(int value) {
            for (int i = 0; i < values().length; i++) {
                BasisType type = values()[i];
                if (type.getValue() == value) {
                    return type;
                }
            }
            return null;
        }
    }

    private BondType type;
    private BasisType basis;
    private Date clearingDate;
    private Date xDate;
    private Date lastPayDate;
    private Date nextPayDate;
    private double accruedInterest;
    private double couponTax;
    private double payCnt;

    private Date date;




    public BondCalculator(BondType type, BasisType basis,
                          Date clearingDate, Date lastPayDate, Date nextPayDate, Date xDate,
                          double accruedInterest,
                          double couponTax, double payCnt) {
        this.type = type;
        this.basis = basis;
        this.clearingDate = clearingDate;
        this.lastPayDate = lastPayDate;
        this.nextPayDate = nextPayDate;
        this.xDate = xDate;
        this.accruedInterest = accruedInterest;
        this.couponTax = couponTax;
        this.payCnt = payCnt;
    }

    public static final double NOMINAL_100 = 100;
    public static final int PERIOD_365 = 365;
    public static final int PERIOD_182 = 182;
    public static final int PERIOD_183 = 183;
    public static final double ACCURACY = 0.001;
    public static final double ROBUST_ACCURACY = 0.25;
    //    public static final double ACCURACY = 0.00001;
    public static final int MAX_ITERATIONS = 1000;

    public CalcResult calcBondPrice(double yield) {
        if (type == BondType.DISCOUNT) {
            return calcDiscountBondPrice(yield);
        } else {
            return calcCouponBondPrice(yield);
        }
    }

    public CalcResult calcCouponBondPrice(double yield) {
        return calcPriceFunction(yield);
    }

    public CalcResult calcDiscountBondPrice(double yield) {
        Date today = new Date();
        int daysInYear = getDaysInYear(basis);
        int daysLeft = getDaysToDate(basis, today, clearingDate);

        double res = 100 / ((yield * daysLeft) / (daysInYear * 100) + 1);
        return new CalcResult(res);
    }


    public CalcResult calcBondYield(double price) {
        if (type == BondType.DISCOUNT) {
            return calcDiscountBondYield(price);
        } else {
            return calcCouponBondYield(price);
        }
    }

    public CalcResult calcDiscountBondYield(double price) {
        if (basis == null || clearingDate == null) return null;
        Date today = new Date();
        int daysInYear = getDaysInYear(basis);
        int daysLeft = getDaysToDate(basis, today, clearingDate);

        double res = (100 - price) * daysInYear * 100 / (price * daysLeft);
        return new CalcResult(res);
    }


    public CalcResult calcCouponBondYield(double price) {
//        double yield = 10;
        double yield = 5;

        double aprox, deriv;
        int cnt = 0;
        do {
            CalcResult prcRes = calcPriceFunction(yield);
            aprox = prcRes.getValue() - price;
            deriv = prcRes.getDerivative();
            yield -= aprox / deriv;
//            System.out.println(yield + "\t" + aprox + "\t" + deriv);

        } while (Math.abs(aprox) > ACCURACY && cnt++ < MAX_ITERATIONS);

        CalcResult res = new CalcResult();
        res.setValue(yield);
        res.setIterations(cnt);
        res.setAccuracy(aprox);
        return res;
    }


    /**
     * Calculates price function value and its derivative value
     *
     * @param yield - yield
     * @return array of direct function value and it's derivative value
     */
    private CalcResult calcPriceFunction(double yield) {

        final int daysInYear = getDaysInYear(basis);

        double value = 0;
        double deriv = 0;
        Date today = new Date();
        date = clearingDate;
        boolean f182 = false;

        double tr = 0;
        while (today.before(date)) {
            double payCnt = basis == BasisType.TYPE_ACTUAL_182_183 ?
                    (double) daysInYear / (f182 ? PERIOD_182 : PERIOD_183) :
                    this.payCnt;

            int daysLeft = getDaysToDate(basis, today, date);

            double pow = ((double) daysLeft / daysInYear) * payCnt;
            double arg = 1 / (1 + yield / 100 / payCnt);
            double coef = couponTax / payCnt + (date.equals(clearingDate) ? NOMINAL_100 : 0);
            double coefDeriv = -coef / ((double) daysInYear / daysLeft);

            tr = Math.pow(arg, pow);
            value += coef * tr;
            deriv += coefDeriv * Math.pow(arg, pow - 1);

            if (basis == BasisType.TYPE_ACTUAL_182_183) {
                date = decreaseDays(date, f182 ? PERIOD_182 : PERIOD_183);
                f182 = !f182;
            } else {
                int daysInPeriod = (int) Math.round((double) daysInYear / payCnt);
                if (daysInYear == 360) {
                    date = decreaseDays30(date, daysInPeriod);
                } else {
                    date = decreaseDays(date, daysInPeriod);
                }
            }
        }

        //todo check this
        if (today.before(nextPayDate) && today.after(xDate) /*&& !xDate.equals(nextPayDate)*/) {
            value -= tr * couponTax / this.payCnt;
        }

        value -= accruedInterest;

        CalcResult result = new CalcResult();
        result.setValue(value);
        result.setDerivative(deriv);
        return result;
    }


    public static Date decreaseDays(Date date, int days) {
        return new Date(date.getTime() - days * MILIS_IN_DAY);
    }

    public static Date decreaseDays30(Date date, int days) {
        Calendar cal = new GregorianCalendar();
        cal.setTime(date);
        int y1 = cal.get(Calendar.YEAR);
        int m1 = cal.get(Calendar.MONTH);
        int d1 = cal.get(Calendar.DAY_OF_MONTH);

        if (days < d1) {
            cal.set(Calendar.DAY_OF_MONTH, d1 - days);
            return cal.getTime();
        }
        if (d1 > 30) {
            d1 = 30;
        }
        int c = (days - d1) / 30;
        int y2 = y1;
        int m2 = m1 - c - 1;
        if (m2 < 1) {
            y2--;
            m2 += 12;
        }
        int d2 = 30 - (days - d1 - c * 30);
        if (m2 == 2 && d2 == 29 && !isLeapYear()) {
            m2 = 3;
            d2 = 1;
        }
        cal.set(Calendar.YEAR, y2);
        cal.set(Calendar.MONTH, m2);
        cal.set(Calendar.DAY_OF_MONTH, d2);
        return cal.getTime();
    }


    public static boolean isLeapYear() {
        Date now = new Date();
        Calendar cal = new GregorianCalendar();
        cal.setTime(now);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
        Date nextYear = cal.getTime();
        long days = (nextYear.getTime() - now.getTime()) / MILIS_IN_DAY;
        return days > 365;
    }


    public static int calcDays360By30(Date fromDate, Date toDate) {
        Calendar c1 = new GregorianCalendar();
        c1.setTime(fromDate);
        int d1 = c1.get(Calendar.DAY_OF_MONTH);
        int m1 = c1.get(Calendar.MONTH);
        int y1 = c1.get(Calendar.YEAR);
        Calendar c2 = new GregorianCalendar();
        c2.setTime(toDate);
        int d2 = c2.get(Calendar.DAY_OF_MONTH);
        int m2 = c2.get(Calendar.MONTH);
        int y2 = c2.get(Calendar.YEAR);

        int years = y2 - y1 - 1;

        int dty1 = d1 < 30 ? 30 - d1 : 0;
        int dty2 = d2 > 30 ? 30 : d2;

        return years * 360 + (12 - m1) * 30 + dty1 + (m2 - 1) * 30 + dty2;
    }


    public static int getDaysToDate(BasisType basisType, Date from, Date to) {
        if (basisType == BasisType.TYPE_30_360) {
            return calcDays360By30(from, to);
        } else {
            return (int) ((to.getTime() - from.getTime()) / MILIS_IN_DAY) + 1;
        }
    }


    public static int getDaysInYear(BasisType basisType) {
        switch (basisType) {
            case TYPE_ACTUAL_365:
            case TYPE_ACTUAL_182_183: {
                return 365;
            }
            case TYPE_30_360: {
                return 360;
            }
            case TYPE_ACTUAL_360: {
                return 360;
            }
            case TYPE_ACTUAL_364: {
                return 364;
            }
            case TYPE_ACTUAL_ACTUAL: {
                return isLeapYear() ? 366 : 365;
            }
            default: {
                return 364;
            }
        }

    }


    public static String decodeDates(String db) {
        String r = "";
        for (int i = 1; i <= db.length(); i += 3) {
            if (db.length() < i + 2) {
                break;
            }
            char d = db.charAt(i);
            char m = db.charAt(i + 1);
            char y = db.charAt(i + 2);
            r = r + (int) (d) + (int) (m) + (int) (y) + ";";
        }
        return r;
    }

    public static class CalcResult {

        private double value;
        private double derivative;
        private double accuracy;
        private int iterations;

        public CalcResult() {
        }

        public CalcResult(double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public double getDerivative() {
            return derivative;
        }

        public void setDerivative(double derivative) {
            this.derivative = derivative;
        }

        public double getAccuracy() {
            return accuracy;
        }

        public void setAccuracy(double accuracy) {
            this.accuracy = accuracy;
        }

        public int getIterations() {
            return iterations;
        }

        public void setIterations(int iterations) {
            this.iterations = iterations;
        }
    }




}
