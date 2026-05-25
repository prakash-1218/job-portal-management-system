package com.jobportal;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    JobRepository repo;

    @Autowired
    ApplicationRepository appRepo;

    @Autowired
    UserRepository userRepo;

    @Autowired
    private JavaMailSender mailSender;

    // HOME PAGE
    @GetMapping("/")
    public String home(Model model, HttpSession session) {

        model.addAttribute("username",
                session.getAttribute("username"));

        model.addAttribute("role",
                session.getAttribute("role"));

        model.addAttribute("photo",
                session.getAttribute("photo"));

        return "index";
    }

    // LOGIN PAGE
    @GetMapping("/login")
    public String loginPage() {

        return "login";
    }

    // LOGIN CHECK
    @PostMapping("/checkLogin")
    public String checkLogin(@RequestParam String email,
                             @RequestParam String password,
                             Model model,
                             HttpSession session) {

        User user =
                userRepo.findByEmailAndPassword(
                        email,
                        password
                );

        if (user != null) {

            session.setAttribute(
                    "username",
                    user.getName()
            );

            session.setAttribute(
                    "role",
                    user.getRole()
            );

            session.setAttribute(
                    "photo",
                    user.getProfilePhoto()
            );

            return "redirect:/";

        } else {

            model.addAttribute(
                    "message",
                    "Invalid Email or Password"
            );

            return "login";
        }
    }

    // LOGOUT
    @GetMapping("/logout")
    public String logout(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }

    // REGISTER PAGE
    @GetMapping("/register")
    public String registerPage() {

        return "register";
    }

    // SAVE USER
    @PostMapping("/saveUser")
    public String saveUser(@ModelAttribute User user,
                           @RequestParam("photo")
                           MultipartFile file) {

        try {

            if (!file.isEmpty()) {

                String uploadDir =
                        System.getProperty("user.dir")
                        + "\\uploads\\profile";

                File folder = new File(uploadDir);

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String fileName =
                        System.currentTimeMillis()
                        + "_"
                        + file.getOriginalFilename();

                File saveFile =
                        new File(
                                uploadDir
                                + "\\"
                                + fileName
                        );

                file.transferTo(saveFile);

                user.setProfilePhoto(fileName);
            }

            userRepo.save(user);

        } catch (Exception e) {

            e.printStackTrace();
        }

        return "redirect:/login";
    }

    // VIEW JOBS
    @GetMapping("/jobs")
    public String jobs(Model model,
                       HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        List<Job> jobs = repo.findAll();

        model.addAttribute("jobs", jobs);

        return "jobs";
    }

    // ADD JOB PAGE
    @GetMapping("/addjob")
    public String addJob(HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        return "addjob";
    }

    // SAVE JOB
    @PostMapping("/saveJob")
    public String saveJob(@ModelAttribute Job job,
                          HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        repo.save(job);

        return "redirect:/jobs";
    }

    // EDIT JOB
    @GetMapping("/editJob/{id}")
    public String editJob(@PathVariable int id,
                          Model model,
                          HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        Job job =
                repo.findById(id).orElse(null);

        model.addAttribute("job", job);

        return "editjob";
    }

    // UPDATE JOB
    @PostMapping("/updateJob")
    public String updateJob(@ModelAttribute Job job,
                            HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        repo.save(job);

        return "redirect:/jobs";
    }

    // DELETE JOB
    @GetMapping("/deleteJob/{id}")
    public String deleteJob(@PathVariable int id,
                            HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        repo.deleteById(id);

        return "redirect:/jobs";
    }

    // APPLY FORM
    @GetMapping("/applyForm/{title}")
    public String applyForm(@PathVariable String title,
                            Model model,
                            HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        model.addAttribute("title", title);

        model.addAttribute(
                "username",
                session.getAttribute("username")
        );

        return "apply";
    }

    // SUBMIT APPLICATION
    @PostMapping("/submitApplication")
    public String submitApplication(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String jobTitle,
            @RequestParam("resume")
            MultipartFile file,
            HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        try {

            if (file.isEmpty()) {

                return "redirect:/jobs";
            }

            // RESUME FOLDER
            String uploadDir =
                    System.getProperty("user.dir")
                    + "\\uploads\\resume";

            File folder = new File(uploadDir);

            if (!folder.exists()) {

                folder.mkdirs();
            }

            // UNIQUE FILE NAME
            String uniqueFileName =
                    System.currentTimeMillis()
                    + "_"
                    + file.getOriginalFilename();

            File saveFile =
                    new File(
                            uploadDir
                            + "\\"
                            + uniqueFileName
                    );

            file.transferTo(saveFile);

            // SAVE APPLICATION
            Application app =
                    new Application();

            app.setApplicantName(
                    name.trim()
            );

            app.setEmail(email);

            app.setJobTitle(jobTitle);

            app.setStatus("APPLIED");

            app.setResumeFile(
                    uniqueFileName
            );

            // DATE
            String currentDate =
                    LocalDate.now().toString();

            // TIME
            String currentTime =
                    LocalTime.now()
                    .format(
                            DateTimeFormatter
                            .ofPattern("hh:mm a")
                    );

            app.setAppliedDate(currentDate);

            app.setAppliedTime(currentTime);

            appRepo.save(app);

        } catch (Exception e) {

            e.printStackTrace();
        }

        return "redirect:/applications";
    }

    // VIEW APPLICATIONS
    @GetMapping("/applications")
    public String viewApplications(
            Model model,
            HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        List<Application> applications =
                appRepo.findAll();

        model.addAttribute(
                "applications",
                applications
        );

        return "applications";
    }

    // SHORTLIST
    @GetMapping("/shortlist/{id}")
    public String shortlist(
            @PathVariable int id,
            HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        Application app =
                appRepo.findById(id)
                .orElse(null);

        if (app != null) {

            app.setStatus("SHORTLISTED");

            appRepo.save(app);

            SimpleMailMessage message =
                    new SimpleMailMessage();

            String companyName =
                    app.getJobTitle();

            message.setFrom(
                    "yourgmail@gmail.com"
            );

            message.setTo(
                    app.getEmail()
            );

            message.setSubject(
                    companyName
                    + " Recruitment Team"
            );

            message.setText(

                    "Dear "
                    + app.getApplicantName()
                    + ",\n\n"

                    + "Congratulations!\n\n"

                    + "Your application for "
                    + app.getJobTitle()
                    + " has been shortlisted.\n\n"

                    + "Applied Date: "
                    + app.getAppliedDate()
                    + "\n"

                    + "Applied Time: "
                    + app.getAppliedTime()
                    + "\n\n"

                    + "Regards,\n"

                    + companyName
                    + " HR Team"
            );

            mailSender.send(message);
        }

        return "redirect:/applications";
    }

    // REJECT
    @GetMapping("/reject/{id}")
    public String reject(@PathVariable int id,
                         HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        Application app =
                appRepo.findById(id)
                .orElse(null);

        if (app != null) {

            app.setStatus("REJECTED");

            appRepo.save(app);

            SimpleMailMessage message =
                    new SimpleMailMessage();

            String companyName =
                    app.getJobTitle();

            message.setFrom(
                    "yourgmail@gmail.com"
            );

            message.setTo(
                    app.getEmail()
            );

            message.setSubject(
                    companyName
                    + " Recruitment Team"
            );

            message.setText(

                    "Dear "
                    + app.getApplicantName()
                    + ",\n\n"

                    + "Thank you for applying for "
                    + app.getJobTitle()
                    + ".\n\n"

                    + "We regret to inform you that "
                    + "your application was not selected.\n\n"

                    + "Regards,\n"

                    + companyName
                    + " HR Team"
            );

            mailSender.send(message);
        }

        return "redirect:/applications";
    }

    // DELETE APPLICATION
    @GetMapping("/deleteApplication/{id}")
    public String deleteApplication(
            @PathVariable int id,
            HttpSession session) {

        if (session.getAttribute("username")
                == null) {

            return "redirect:/login";
        }

        appRepo.deleteById(id);

        return "redirect:/applications";
    }

    // VIEW RESUME
    @GetMapping("/uploads/resume/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getResume(
            @PathVariable String filename)
            throws Exception {

        String uploadDir =
                System.getProperty("user.dir")
                + "\\uploads\\resume";

        File file =
                new File(uploadDir, filename);

        Resource resource =
                new UrlResource(file.toURI());

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                        + file.getName()
                        + "\""
                )
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .body(resource);
    }

    // VIEW PROFILE PHOTO
    @GetMapping("/uploads/profile/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> getProfile(
            @PathVariable String filename)
            throws Exception {

        String uploadDir =
                System.getProperty("user.dir")
                + "\\uploads\\profile";

        File file =
                new File(uploadDir, filename);

        Resource resource =
                new UrlResource(file.toURI());

        return ResponseEntity.ok()
                .contentType(
                        MediaType.IMAGE_JPEG
                )
                .body(resource);
    }

    // FORGOT PASSWORD PAGE
    @GetMapping("/forgotPassword")
    public String forgotPasswordPage() {

        return "forgotpassword";
    }

    // FORGOT PASSWORD CHECK
    @PostMapping("/forgotPassword")
    public String forgotPassword(
            @RequestParam String email,
            Model model) {

        User user =
                userRepo.findByEmail(email);

        if (user != null) {

            model.addAttribute(
                    "message",
                    "Your Password is: "
                    + user.getPassword()
            );

        } else {

            model.addAttribute(
                    "message",
                    "Email not found"
            );
        }

        return "forgotpassword";
    }
}