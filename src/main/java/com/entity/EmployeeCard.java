package com.entity;

import java.time.LocalDate;

/** empno 同时作为主键和外键，数据库保证每位员工最多一张工牌。 */
public class EmployeeCard {
    private Integer empno;
    private String cardNumber;
    private LocalDate issuedOn;

    public Integer getEmpno() { return empno; }
    public void setEmpno(Integer empno) { this.empno = empno; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public void setIssuedOn(LocalDate issuedOn) { this.issuedOn = issuedOn; }
}
