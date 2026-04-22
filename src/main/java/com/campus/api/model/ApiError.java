/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.campus.api.model;

/**
 *
 * @author Basilu
 */


public class ApiError {

    private int statusCode;
    private String errorType;
    private String detail;
    private long occurredAt;

    public ApiError(int statusCode, String errorType, String detail) {
        this.statusCode = statusCode;
        this.errorType = errorType;
        this.detail = detail;
        this.occurredAt = System.currentTimeMillis();
    }

    public int getStatusCode() { return statusCode; }
    public String getErrorType() { return errorType; }
    public String getDetail() { return detail; }
    public long getOccurredAt() { return occurredAt; }
}
