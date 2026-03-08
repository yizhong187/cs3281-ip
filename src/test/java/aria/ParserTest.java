package aria;

import aria.command.Command;
import aria.command.CommandType;
import aria.command.Parser;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests for the Parser class. */
public class ParserTest {

    @Test
    public void parse_bye_returnsExitCommand() throws AriaException {
        Command cmd = Parser.parse("bye");
        assertEquals(CommandType.BYE, cmd.getType());
        assertTrue(cmd.isExit());
    }

    @Test
    public void parse_list_returnsListCommand() throws AriaException {
        Command cmd = Parser.parse("list");
        assertEquals(CommandType.LIST, cmd.getType());
        assertFalse(cmd.isExit());
    }

    @Test
    public void parse_help_returnsHelpCommand() throws AriaException {
        Command cmd = Parser.parse("help");
        assertEquals(CommandType.HELP, cmd.getType());
    }

    @Test
    public void parse_todo_validDesc_returnsTodoCommand() throws AriaException {
        Command cmd = Parser.parse("todo read book");
        assertEquals(CommandType.TODO, cmd.getType());
    }

    @Test
    public void parse_todo_emptyDesc_throwsAriaException() {
        assertThrows(AriaException.class, () -> Parser.parse("todo"));
        assertThrows(AriaException.class, () -> Parser.parse("todo   "));
    }

    @Test
    public void parse_deadline_validInput_returnsDeadlineCommand() throws AriaException {
        Command cmd = Parser.parse("deadline submit report /by 2024-12-01");
        assertEquals(CommandType.DEADLINE, cmd.getType());
    }

    @Test
    public void parse_deadline_missingBy_throwsAriaException() {
        assertThrows(AriaException.class, () -> Parser.parse("deadline submit report"));
    }

    @Test
    public void parse_event_validInput_returnsEventCommand() throws AriaException {
        Command cmd = Parser.parse("event project meeting /from Mon 2pm /to 4pm");
        assertEquals(CommandType.EVENT, cmd.getType());
    }

    @Test
    public void parse_event_missingFrom_throwsAriaException() {
        assertThrows(AriaException.class, () -> Parser.parse("event project meeting /to 4pm"));
    }

    @Test
    public void parse_event_missingTo_throwsAriaException() {
        assertThrows(AriaException.class,
                () -> Parser.parse("event project meeting /from Mon 2pm"));
    }

    @Test
    public void parse_mark_validNumber_returnsMarkCommand() throws AriaException {
        Command cmd = Parser.parse("mark 1");
        assertEquals(CommandType.MARK, cmd.getType());
    }

    @Test
    public void parse_mark_noNumber_throwsAriaException() {
        assertThrows(AriaException.class, () -> Parser.parse("mark"));
    }

    @Test
    public void parse_delete_validNumber_returnsDeleteCommand() throws AriaException {
        Command cmd = Parser.parse("delete 2");
        assertEquals(CommandType.DELETE, cmd.getType());
    }

    @Test
    public void parse_find_validKeyword_returnsFindCommand() throws AriaException {
        Command cmd = Parser.parse("find book");
        assertEquals(CommandType.FIND, cmd.getType());
    }

    @Test
    public void parse_unknown_throwsAriaException() {
        assertThrows(AriaException.class, () -> Parser.parse("foobar"));
    }

    @Test
    public void parse_caseInsensitive_parsesCorrectly() throws AriaException {
        assertEquals(CommandType.LIST, Parser.parse("LIST").getType());
        assertEquals(CommandType.BYE, Parser.parse("BYE").getType());
        assertEquals(CommandType.HELP, Parser.parse("Help").getType());
    }

    @Test
    public void parse_extraWhitespace_parsesCorrectly() throws AriaException {
        assertEquals(CommandType.LIST, Parser.parse("  list  ").getType());
        assertEquals(CommandType.BYE, Parser.parse("  bye  ").getType());
    }
}
