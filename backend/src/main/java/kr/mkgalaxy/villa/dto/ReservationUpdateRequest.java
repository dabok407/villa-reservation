package kr.mkgalaxy.villa.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

public class ReservationUpdateRequest {

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    @NotNull(message = "입실일은 필수입니다")
    private LocalDate checkInDate;

    @NotNull(message = "퇴실일은 필수입니다")
    private LocalDate checkOutDate;

    private int adultCount;
    private int childCount;
    private String description;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDate getCheckInDate() { return checkInDate; }
    public void setCheckInDate(LocalDate checkInDate) { this.checkInDate = checkInDate; }

    public LocalDate getCheckOutDate() { return checkOutDate; }
    public void setCheckOutDate(LocalDate checkOutDate) { this.checkOutDate = checkOutDate; }

    public int getAdultCount() { return adultCount; }
    public void setAdultCount(int adultCount) { this.adultCount = adultCount; }

    public int getChildCount() { return childCount; }
    public void setChildCount(int childCount) { this.childCount = childCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
