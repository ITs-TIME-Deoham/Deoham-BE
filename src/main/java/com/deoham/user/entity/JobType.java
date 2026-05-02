package com.deoham.user.entity;

public enum JobType {
    DEVELOPER("개발자"),
    DESIGNER("디자이너"),
    MARKETER("마케터"),
    PLANNER("기획자"),
    PHOTOGRAPHER("사진/영상"),
    WRITER("작가/카피라이터"),
    ILLUSTRATOR("일러스트레이터"),
    TRANSLATOR("번역가"),
    CONSULTANT("컨설턴트"),
    EDUCATOR("강사/튜터"),
    OTHER("기타");

    private final String displayName;

    JobType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
