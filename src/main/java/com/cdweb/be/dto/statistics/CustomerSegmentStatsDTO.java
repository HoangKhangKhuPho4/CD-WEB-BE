package com.cdweb.be.dto.statistics;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerSegmentStatsDTO {

  private List<SegmentBreakdown> segments;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SegmentBreakdown {
    private Integer productTypeId; // ID nhóm danh mục
    private String categoryName; // Tên đồ chơi/Thiết bị
    private String segmentLabel; // "iFan", "Gamer", "Gia Đình"
    private Long userCount; // Số người dùng thuộc nhóm
    private Double percentage; // Tỷ lệ phân bổ
    private String color; // Mã màu biểu đồ
  }
}
