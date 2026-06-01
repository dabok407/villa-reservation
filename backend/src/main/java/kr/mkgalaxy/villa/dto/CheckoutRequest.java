package kr.mkgalaxy.villa.dto;

import javax.validation.constraints.NotBlank;

public class CheckoutRequest {

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    private String memo;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
