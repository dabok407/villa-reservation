package kr.mkgalaxy.villa.dto;

import javax.validation.constraints.NotBlank;

public class PasswordVerifyRequest {

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
