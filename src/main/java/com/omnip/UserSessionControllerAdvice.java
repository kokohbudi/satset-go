package com.omnip;

import com.omnip.dtos.UserDTO;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class UserSessionControllerAdvice {
    private final UserDTO userDTO;

    public UserSessionControllerAdvice(UserDTO userDTO) {
        this.userDTO = userDTO;
    }

    @ModelAttribute
    public void addAttributes(Model model) {
        model.addAttribute("user", this.userDTO);
    }
}
