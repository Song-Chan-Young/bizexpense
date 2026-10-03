package com.bizexpense.global.config;

import com.bizexpense.domain.approval.ApprovalService;
import com.bizexpense.domain.approval.ApprovalStatus;
import com.bizexpense.domain.approval.ApprovalTargetType;
import com.bizexpense.domain.code.ExpenseCategoryRepository;
import com.bizexpense.domain.code.PaymentMethodRepository;
import com.bizexpense.domain.department.Department;
import com.bizexpense.domain.department.DepartmentRepository;
import com.bizexpense.domain.expense.ExpenseService;
import com.bizexpense.domain.expense.dto.ExpenseRequest;
import com.bizexpense.domain.schedule.ScheduleService;
import com.bizexpense.domain.schedule.ScheduleType;
import com.bizexpense.domain.schedule.dto.ScheduleRequest;
import com.bizexpense.domain.trip.TripService;
import com.bizexpense.domain.trip.dto.TripRequest;
import com.bizexpense.domain.user.Role;
import com.bizexpense.domain.user.User;
import com.bizexpense.domain.user.UserRepository;
import com.bizexpense.global.security.LoginUser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로컬 개발 / 데모(체험) 서버용 초기 데이터. 사용자가 한 명도 없을 때만 넣는다.
 * 날짜는 실행한 날 기준으로 만들어서 언제 접속해도 "이번 주 일정"에 데이터가 보이게 한다.
 * 출장·결재·일정·경비는 서비스 계층을 그대로 거쳐 만들어 업무 규칙과 같은 상태가 된다.
 */
@Slf4j
@Order(1)
@Component
@Profile({"local", "demo"})
@RequiredArgsConstructor
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoProperties demoProperties;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpenseCategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final TripService tripService;
    private final ApprovalService approvalService;
    private final ScheduleService scheduleService;
    private final ExpenseService expenseService;

    private final Map<String, LoginUser> users = new HashMap<>();
    private final Map<String, Long> categories = new HashMap<>();
    private final Map<String, Long> payments = new HashMap<>();
    private LocalDate today;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            return;
        }
        today = LocalDate.now();
        categoryRepository.findAll().forEach(c -> categories.put(c.getName(), c.getId()));
        paymentMethodRepository.findAll().forEach(p -> payments.put(p.getName(), p.getId()));

        createUsers();
        createTrips();
        createSchedules();

        log.info("데모 데이터 생성 완료: admin / manager1 / user1 / user2 / manager2 / user3 (비밀번호 {})",
                demoProperties.password());
    }

    private void createUsers() {
        Department mgt = departmentRepository.save(new Department("경영지원팀", "MGT"));
        Department sales1 = departmentRepository.save(new Department("영업1팀", "SALES1"));
        Department sales2 = departmentRepository.save(new Department("영업2팀", "SALES2"));

        String encoded = passwordEncoder.encode(demoProperties.password());
        user(mgt, "admin", "관리자", Role.ADMIN, encoded);
        user(sales1, "manager1", "김팀장", Role.MANAGER, encoded);
        user(sales1, "user1", "이사원", Role.USER, encoded);
        user(sales1, "user2", "박사원", Role.USER, encoded);
        user(sales2, "manager2", "최팀장", Role.MANAGER, encoded);
        user(sales2, "user3", "정사원", Role.USER, encoded);
    }

    private void createTrips() {
        // 1) 진행중 출장 + 일정 + 경비 (어제 ~ 내일)
        long busan = trip("user1", "부산 거래처 방문", "A사 신규 계약, B사 제품 시연", "부산",
                -1, 1, 600_000, true);
        approve("manager1", busan, "잘 다녀오세요.");
        tripService.start(users.get("user1"), busan);

        schedule("user1", busan, ScheduleType.TRIP, "서울 → 부산 KTX 이동", "서울역", -1, "07:00", "09:40");
        schedule("user1", busan, ScheduleType.CLIENT_VISIT, "A사 신규 계약 미팅", "부산 A사 본사", -1, "14:00", "16:00");
        schedule("user1", busan, ScheduleType.CLIENT_VISIT, "B사 제품 시연", "부산 B사 연구소", 0, "10:00", "11:30");
        schedule("user1", busan, ScheduleType.TRIP, "부산 → 서울 KTX 이동", "부산역", 1, "15:00", "17:40");

        expense("user1", busan, "교통비", "법인카드", -1, "KTX 서울→부산", 59_800);
        expense("user1", busan, "식비", "개인카드", -1, "부산 돼지국밥", 9_000);
        expense("user1", busan, "숙박비", "개인카드", -1, "해운대 비즈니스 호텔", 98_000);
        expense("user1", busan, "접대비", "법인카드", -1, "A사 미팅 저녁", 186_000);
        expense("user1", busan, "식비", "법인카드", 0, "B사 미팅 점심", 45_000);
        expense("user1", busan, "교통비", "현금", 0, "택시 (호텔→B사)", 12_300);

        // 2) 결재 대기 (팀장 결재함에 표시)
        trip("user1", "대전 고객사 교육", "신규 고객사 대상 솔루션 사용 교육", "대전", 7, 8, 250_000, true);

        // 3) 반려 → 사유 확인 후 수정/재신청 체험
        long gwangju = trip("user2", "광주 지사 점검", "분기 지사 운영 점검", "광주", 3, 4, 1_200_000, true);
        reject("manager1", gwangju, "숙박비 기준(1박 10만원)에 맞춰 예상 경비를 다시 산정해주세요.");

        // 4) 임시저장
        trip("user1", "제주 워크숍 준비", "하반기 영업 워크숍 사전 답사", "제주", 20, 22, 900_000, false);

        // 5) 완료된 출장 + 경비
        long incheon = trip("user2", "인천 물류센터 방문", "물류 프로세스 개선 협의", "인천", -10, -10, 80_000, true);
        approve("manager1", incheon, null);
        tripService.start(users.get("user2"), incheon);
        expense("user2", incheon, "교통비", "개인카드", -10, "주차비", 8_000);
        expense("user2", incheon, "식비", "개인카드", -10, "물류센터 구내식당", 7_500);
        tripService.complete(users.get("user2"), incheon);

        // 6) 다른 팀 결재 대기 (영업2팀 팀장에게만 보임)
        trip("user3", "울산 공장 미팅", "부품 납품 일정 협의", "울산", 5, 6, 450_000, true);
    }

    private void createSchedules() {
        schedule("user1", null, ScheduleType.MEETING, "주간 영업 회의", "3층 대회의실", 2, "09:30", "10:30");
        schedule("user1", null, ScheduleType.TRAINING, "정보보안 교육", "본사 교육장", 2, "14:00", "17:00");
        schedule("user1", null, ScheduleType.OUTSIDE_WORK, "은행 업무", "여의도 지점", 3, "11:00", "12:00");
        schedule("user2", null, ScheduleType.MEETING, "신규 고객 제안서 리뷰", "2층 회의실", 0, "10:00", "11:00");
        schedule("user2", null, ScheduleType.CLIENT_VISIT, "C사 정기 미팅", "강남 C사", 1, "14:00", "15:30");
        schedule("user2", null, ScheduleType.MEETING, "주간 영업 회의", "3층 대회의실", 2, "09:30", "10:30");
        schedule("manager1", null, ScheduleType.MEETING, "팀장 회의", "임원 회의실", 0, "16:00", "17:00");
        schedule("manager1", null, ScheduleType.MEETING, "주간 영업 회의", "3층 대회의실", 2, "09:30", "10:30");
        schedule("manager1", null, ScheduleType.PERSONAL, "분기 실적 보고서 작성", null, 4, "13:00", "18:00");
        schedule("user3", null, ScheduleType.CLIENT_VISIT, "D사 견적 협의", "판교 D사", 1, "10:00", "11:00");
    }

    // ---------- helpers ----------

    private void user(Department dept, String loginId, String name, Role role, String encodedPassword) {
        User user = userRepository.save(User.builder()
                .department(dept)
                .loginId(loginId)
                .password(encodedPassword)
                .name(name)
                .email(loginId + "@bizexpense.demo")
                .role(role)
                .hireDate(LocalDate.of(2024, 1, 2))
                .build());
        users.put(loginId, new LoginUser(user.getId(), loginId, role, dept.getId()));
    }

    private long trip(String owner, String title, String purpose, String destination,
                      int startOffset, int endOffset, long expected, boolean submit) {
        return tripService.create(users.get(owner), new TripRequest(title, purpose, destination,
                today.plusDays(startOffset), today.plusDays(endOffset), expected, submit)).tripId();
    }

    private void approve(String approver, long tripId, String comment) {
        approvalService.approve(users.get(approver), pendingApprovalId(approver, tripId), comment);
    }

    private void reject(String approver, long tripId, String comment) {
        approvalService.reject(users.get(approver), pendingApprovalId(approver, tripId), comment);
    }

    private long pendingApprovalId(String viewer, long tripId) {
        return approvalService.history(ApprovalTargetType.TRIP, tripId, users.get(viewer).userId()).stream()
                .filter(a -> a.status() == ApprovalStatus.PENDING)
                .findFirst()
                .orElseThrow()
                .approvalId();
    }

    private void schedule(String owner, Long tripId, ScheduleType type, String title, String location,
                          int dayOffset, String start, String end) {
        LocalDate day = today.plusDays(dayOffset);
        scheduleService.create(users.get(owner), new ScheduleRequest(type, title, null,
                LocalDateTime.of(day, LocalTime.parse(start)), LocalDateTime.of(day, LocalTime.parse(end)),
                location, tripId, null, true));
    }

    private void expense(String owner, long tripId, String category, String payment, int dayOffset,
                         String store, long amount) {
        expenseService.create(users.get(owner), new ExpenseRequest(tripId, categories.get(category),
                payments.get(payment), today.plusDays(dayOffset), store, amount, null));
    }
}
