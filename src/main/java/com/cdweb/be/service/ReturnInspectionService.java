package com.cdweb.be.service;

import com.cdweb.be.dto.ReturnInspectionDto;
import java.util.List;

public interface ReturnInspectionService {

  List<ReturnInspectionDto.SheetSummary> listPending();

  List<ReturnInspectionDto.SheetSummary> listDrafts();

  List<ReturnInspectionDto.SheetSummary> listProcessed();

  ReturnInspectionDto.SheetDetail getDetail(Integer id);

  ReturnInspectionDto.DefectLabelResponse getDefectLabel(Integer id);

  ReturnInspectionDto.IntakeResponse intake(String code, String username);

  ReturnInspectionDto.SheetDetail saveDraft(
      Integer id, ReturnInspectionDto.DraftRequest request, String username);

  ReturnInspectionDto.SheetDetail cancel(
      Integer id, ReturnInspectionDto.CancelRequest request, String username);

  ReturnInspectionDto.SheetDetail process(
      Integer id, ReturnInspectionDto.ProcessRequest request, String username);

  void createSheetsForOrder(Integer orderId, String username);
}
