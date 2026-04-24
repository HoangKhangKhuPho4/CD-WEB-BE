package com.cdweb.be.dto.statistics;

import java.util.List;

public class TopProductsStatisticsDTO {
  private List<TopProductDTO> topProducts;

  // getters and setters
  public List<TopProductDTO> getTopProducts() {
    return topProducts;
  }

  public void setTopProducts(List<TopProductDTO> topProducts) {
    this.topProducts = topProducts;
  }
}
