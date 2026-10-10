package com.travelhub;

import com.travelhub.common.exception.BusinessException;
import com.travelhub.common.util.Pages;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.*;

class PagesTest {
    @Test void boundsPageSizeAndRejectsUnapprovedProperties() {
        var page = Pages.checked(PageRequest.of(0, 1000, Sort.by("name")), "id", "name");
        assertThat(page.getPageSize()).isEqualTo(100);
        assertThat(page.getSort().getOrderFor("id")).isNotNull();
        assertThatThrownBy(() -> Pages.checked(PageRequest.of(0, 20, Sort.by("password")), "id", "name"))
                .isInstanceOf(BusinessException.class);
    }

    @Test void treatsUserSearchWildcardsAsLiteralText() {
        assertThat(Pages.contains("  100%_Lake  ")).isEqualTo("%100\\%\\_lake%");
    }
}
