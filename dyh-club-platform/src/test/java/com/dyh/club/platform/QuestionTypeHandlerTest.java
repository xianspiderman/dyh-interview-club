package com.dyh.club.platform.question;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class QuestionTypeHandlerTest{
 @Test void factoryRejectsUnsupportedTypes(){List<QuestionTypeHandler> handlers=new ArrayList<>();for(String type:Arrays.asList("RADIO","MULTIPLE","JUDGE","BRIEF")){QuestionTypeHandler handler=mock(QuestionTypeHandler.class);when(handler.type()).thenReturn(type);handlers.add(handler);}QuestionTypeHandlerFactory factory=new QuestionTypeHandlerFactory(handlers);assertThatThrownBy(()->factory.get("UNKNOWN")).hasMessageContaining("不支持");}
 @Test void multipleChoiceNormalizesOrderingAndDuplicates(){MultipleQuestionHandler handler=new MultipleQuestionHandler(mock(JdbcTemplate.class));assertThat(handler.normalize(" C, A A ")).containsExactly("A","C");}
}
