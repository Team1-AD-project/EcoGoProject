package com.example.EcoGo.dto;

import java.util.List;

public class BatchStockUpdateRequest {
    
    public static class StockUpdateItem {
        private String goodsId;
        private Integer stockChange;  // 正数增加，负数减少
        private Integer newStock;     // 或直接设置新库存
        
        // Getters and Setters
        public String getGoodsId() { return goodsId; }
        public void setGoodsId(String goodsId) { this.goodsId = goodsId; }
        
        public Integer getStockChange() { return stockChange; }
        public void setStockChange(Integer stockChange) { this.stockChange = stockChange; }
        
        public Integer getNewStock() { return newStock; }
        public void setNewStock(Integer newStock) { this.newStock = newStock; }
    }
    
    private List<StockUpdateItem> updates;
    private String reason;  // 操作原因：采购入库、销售出库、盘点调整等
    
    // Getters and Setters
    public List<StockUpdateItem> getUpdates() { return updates; }
    public void setUpdates(List<StockUpdateItem> updates) { this.updates = updates; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}