package com.line1.booktalk.domain.book;

import com.line1.booktalk.domain.analysis.ExpressionType;
import com.line1.booktalk.domain.analysis.PageExpression;
import com.line1.booktalk.domain.analysis.PageExpressionRepository;
import com.line1.booktalk.domain.child.Child;
import com.line1.booktalk.domain.child.ChildRepository;
import com.line1.booktalk.domain.child.Relationship;
import com.line1.booktalk.domain.user.User;
import com.line1.booktalk.domain.user.UserRepository;
import com.line1.booktalk.global.common.HashUtils;
import com.line1.booktalk.global.common.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 앱 시작 시 프론트 시연 화면과 동일한 DEMO 도서/표현을 멱등하게 등록한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DemoBookSeeder implements ApplicationRunner {

    private static final String SEED_DEVICE_HASH = HashUtils.sha256("booktalk-demo-seed-owner-v1");

    private final UserRepository userRepository;
    private final ChildRepository childRepository;
    private final BookRepository bookRepository;
    private final BookPageRepository pageRepository;
    private final PageExpressionRepository expressionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User seedOwner = userRepository.findByDeviceIdHash(SEED_DEVICE_HASH)
                .orElseGet(() -> userRepository.save(User.createGuest(SEED_DEVICE_HASH)));
        Child seedChild = childRepository.findAllByUserIdOrderByCreatedAtAsc(seedOwner.getId()).stream()
                .findFirst()
                .orElseGet(() -> childRepository.save(Child.create(
                        seedOwner, "DEMO", 5, Relationship.GUARDIAN, Language.VI)));

        for (BookSeed seed : books()) {
            seedBook(seedOwner, seedChild, seed);
        }
    }

    private void seedBook(User owner, Child child, BookSeed seed) {
        Book book = bookRepository.findBySourceAndTitle(BookSource.DEMO, seed.title())
                .orElseGet(() -> bookRepository.save(Book.createDemo(seed.title(), "북톡", 4, 7, null)));
        BookPage page = pageRepository.findFirstByBookIdOrderByCreatedAtAsc(book.getId())
                .orElseGet(() -> createDemoPage(book, owner, child, seed.pageLabel()));

        List<PageExpression> existing = expressionRepository.findAllByPageBookIdOrderByCreatedAtAsc(book.getId());
        Set<String> existingTexts = existing.stream().map(PageExpression::getText).collect(Collectors.toSet());
        int added = 0;
        for (WordSeed word : seed.words()) {
            if (existingTexts.add(word.text())) {
                expressionRepository.save(PageExpression.create(
                        page, word.text(), word.type(), word.definition(), Language.VI,
                        word.translation(), word.sourceSentence(), 1.0));
                added++;
            }
        }
        int total = existing.size() + added;
        if (book.getExpressionCount() != total) {
            book.changeExpressionCount(total);
        }
        log.info("DEMO 도서 시드 확인 title={} expressions={}", book.getTitle(), total);
    }

    private BookPage createDemoPage(Book book, User owner, Child child, String pageLabel) {
        BookPage page = BookPage.create(book, owner, child, pageLabel,
                null, "application/x-booktalk-demo", 0, null);
        page.markAnalyzed(null);
        page.markOriginalDeleted();
        return pageRepository.save(page);
    }

    private List<BookSeed> books() {
        return List.of(
                new BookSeed("토끼의 당근 밭", "4쪽", List.of(
                        word("두근두근", ExpressionType.MIMETIC, "가슴이 자꾸 세게 뛰는 모양", "Tim đập thình thịch", "선물 열기 전엔 마음이 두근두근하지!"),
                        word("퐁당", ExpressionType.ONOMATOPOEIA, "작은 물건이 물에 빠질 때 나는 소리", "Tõm", "돌멩이를 던지니까 퐁당 소리가 났어요."),
                        word("엉엉", ExpressionType.ONOMATOPOEIA, "크게 소리 내어 우는 모양", "Hu hu (khóc)", "아기 곰이 엉엉 울었어요."),
                        word("반짝반짝", ExpressionType.MIMETIC, "작은 빛이 계속 빛나는 모양", "Lấp lánh", "밤하늘에 별이 반짝반짝 빛나요."),
                        word("눈이 휘둥그레지다", ExpressionType.IDIOM, "깜짝 놀라서 눈을 크게 뜨다", "Mắt tròn xoe", "커다란 케이크를 보고 눈이 휘둥그레졌어요.")
                )),
                new BookSeed("곰곰이의 비 오는 날", "1쪽", List.of(
                        word("첨벙첨벙", ExpressionType.ONOMATOPOEIA, "물속에서 움직일 때 자꾸 나는 큰 물소리", "Bì bõm", "웅덩이에서 첨벙첨벙 놀자!"),
                        word("퐁당", ExpressionType.ONOMATOPOEIA, "작은 물건이 물에 빠질 때 나는 소리", "Tõm", "돌멩이를 던지니까 퐁당 소리가 났어요."),
                        word("엉금엉금", ExpressionType.MIMETIC, "몸을 낮추고 느리게 기어가는 모양", "Bò chậm chạp", "거북이가 엉금엉금 기어가요."),
                        word("꾸벅꾸벅", ExpressionType.MIMETIC, "졸려서 고개가 자꾸 앞으로 숙여지는 모양", "Gà gật", "강아지가 꾸벅꾸벅 졸아요.")
                )),
                new BookSeed("토토의 밤 소풍", "1쪽", List.of(
                        word("두근두근", ExpressionType.MIMETIC, "가슴이 자꾸 세게 뛰는 모양", "Tim đập thình thịch", "내일 소풍 생각하니 두근두근하다~"),
                        word("깡충깡충", ExpressionType.MIMETIC, "두 발을 모으고 가볍게 자꾸 뛰는 모양", "Nhảy tưng tưng", "토끼가 깡충깡충 뛰어가요."),
                        word("살금살금", ExpressionType.MIMETIC, "남이 모르게 조용히 천천히 움직이는 모양", "Rón rén", "아빠가 자니까 살금살금 걸어가자."),
                        word("데굴데굴", ExpressionType.MIMETIC, "둥근 물건이 계속 굴러가는 모양", "Lăn lông lốc", "공이 데굴데굴 굴러가네!"),
                        word("반짝반짝", ExpressionType.MIMETIC, "작은 빛이 계속 빛나는 모양", "Lấp lánh", "밤하늘에 별이 반짝반짝 빛나요.")
                )),
                new BookSeed("해와 달이 된 오누이", "1쪽", List.of(
                        word("구불구불", ExpressionType.MIMETIC, "이리저리 여러 번 굽은 모양", "Quanh co, ngoằn ngoèo", "구불구불한 산길을 걸어요."),
                        word("꿀꺽", ExpressionType.ONOMATOPOEIA, "음식이나 침을 한꺼번에 삼키는 소리", "Ực (nuốt)", "호랑이가 떡을 꿀꺽 삼켰어요."),
                        word("살금살금", ExpressionType.MIMETIC, "남이 모르게 조용히 천천히 움직이는 모양", "Rón rén", "고양이가 살금살금 다가와요."),
                        word("엉엉", ExpressionType.ONOMATOPOEIA, "크게 소리 내어 우는 모양", "Hu hu (khóc)", "오누이가 엉엉 울었어요."),
                        word("아등바등", ExpressionType.MIMETIC, "무엇을 하려고 몹시 애쓰는 모양", "Cố gắng vật lộn", "오누이가 바위를 아등바등 밀었어요."),
                        word("영차", ExpressionType.INTERJECTION, "여럿이 힘을 모을 때 내는 소리", "Hò dô ta", "영차 영차, 다 같이 당겨요!")
                ))
        );
    }

    private WordSeed word(String text, ExpressionType type, String definition,
                          String translation, String sourceSentence) {
        return new WordSeed(text, type, definition, translation, sourceSentence);
    }

    private record BookSeed(String title, String pageLabel, List<WordSeed> words) {
    }

    private record WordSeed(String text, ExpressionType type, String definition,
                            String translation, String sourceSentence) {
    }
}
