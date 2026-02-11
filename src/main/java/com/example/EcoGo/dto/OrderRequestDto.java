package com.example.EcoGo.dto;

import com.example.EcoGo.model.Order;

import java.util.List;

/**
 * DTO for Order create/update requests.
 * Replaces direct use of the persistent Order entity in @RequestBody (SonarQube S4684).
 */
public class OrderRequestDto {
    private String userId;
    private List<Order.OrderItem> items;
    private Double totalAmount;
    private Double shippingFee;
    private Double finalAmount;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private String remark;
    private String trackingNumber;
    private String carrier;
    private Integer pointsUsed;
    private Integer pointsEarned;

    public Order toEntity() {
        Order order = new Order();
        order.setUserId(this.userId);
        order.setItems(this.items);
        order.setTotalAmount(this.totalAmount);
        order.setShippingFee(this.shippingFee);
        order.setFinalAmount(this.finalAmount);
        order.setStatus(this.status);
        order.setPaymentMethod(this.paymentMethod);
        order.setPaymentStatus(this.paymentStatus);
        order.setShippingAddress(this.shippingAddress);
        order.setRecipientName(this.recipientName);
        order.setRecipientPhone(this.recipientPhone);
        order.setRemark(this.remark);
        order.setTrackingNumber(this.trackingNumber);
        order.setCarrier(this.carrier);
        order.setPointsUsed(this.pointsUsed);
        order.setPointsEarned(this.pointsEarned);
        return order;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<Order.OrderItem> getItems() { return items; }
    public void setItems(List<Order.OrderItem> items) { this.items = items; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public Double getShippingFee() { return shippingFee; }
    public void setShippingFee(Double shippingFee) { this.shippingFee = shippingFee; }

    public Double getFinalAmount() { return finalAmount; }
    public void setFinalAmount(Double finalAmount) { this.finalAmount = finalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getRecipientPhone() { return recipientPhone; }
    public void setRecipientPhone(String recipientPhone) { this.recipientPhone = recipientPhone; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }

    public String getCarrier() { return carrier; }
    public void setCarrier(String carrier) { this.carrier = carrier; }

    public Integer getPointsUsed() { return pointsUsed; }
    public void setPointsUsed(Integer pointsUsed) { this.pointsUsed = pointsUsed; }

    public Integer getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(Integer pointsEarned) { this.pointsEarned = pointsEarned; }
}
