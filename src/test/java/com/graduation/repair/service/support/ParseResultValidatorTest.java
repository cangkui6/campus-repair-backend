package com.graduation.repair.service.support;

import com.graduation.repair.common.exception.BizException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParseResultValidatorTest {

    private final ParseResultValidator validator = new ParseResultValidator();

    @Test
    void validateOrThrow_shouldAcceptShortChineseFaultWithUsefulContext() {
        ParsedTicketData data = ParsedTicketData.builder()
                .category("AIR_CONDITIONER")
                .location("呈贡校区楠苑6栋A座302宿舍")
                .faultPhenomenon("漏水")
                .urgency("MEDIUM")
                .confidence(0.9)
                .build();

        Assertions.assertDoesNotThrow(() -> validator.validateOrThrow(data));
    }

    @Test
    void validateOrThrow_shouldAcceptCommonShortFaultsWithContext() {
        for (String fault : new String[]{"断电", "异响", "堵塞", "跳闸", "掉线", "不亮"}) {
            ParsedTicketData data = ParsedTicketData.builder()
                    .category("WATER_ELECTRIC")
                    .location("呈贡校区教学楼")
                    .faultPhenomenon(fault)
                    .urgency("MEDIUM")
                    .confidence(0.8)
                    .build();

            Assertions.assertDoesNotThrow(() -> validator.validateOrThrow(data), fault + " should be accepted");
        }
    }

    @Test
    void validateOrThrow_shouldRejectShortFaultWithoutContext() {
        ParsedTicketData data = ParsedTicketData.builder()
                .category("OTHER")
                .location("未知位置")
                .faultPhenomenon("断电")
                .urgency("MEDIUM")
                .confidence(0.8)
                .build();

        Assertions.assertThrows(BizException.class, () -> validator.validateOrThrow(data));
    }

    @Test
    void validateOrThrow_shouldRejectMeaninglessText() {
        ParsedTicketData data = ParsedTicketData.builder()
                .category("OTHER")
                .location("未知位置")
                .faultPhenomenon("测试")
                .urgency("MEDIUM")
                .confidence(0.9)
                .build();

        Assertions.assertThrows(BizException.class, () -> validator.validateOrThrow(data));
    }

    @Test
    void validateOrThrow_shouldRejectUnknownLocationAndOtherCategory() {
        ParsedTicketData data = ParsedTicketData.builder()
                .category("OTHER")
                .location("未知位置")
                .faultPhenomenon("设备无法正常使用")
                .urgency("MEDIUM")
                .confidence(0.9)
                .build();

        Assertions.assertThrows(BizException.class, () -> validator.validateOrThrow(data));
    }
}
