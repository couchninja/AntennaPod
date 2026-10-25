package de.danoeh.antennapod.parser.podcastsegments;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;

public class PodcastSegmentsFormatterTest {

    @Test
    public void formatSummaryForShownotes_plainTextNewlines() throws Exception {
        String formatted = invokeFormatSummary("Line one\nLine two");
        assertEquals("<p>Line one<br />Line two</p>", formatted);
    }

    @Test
    public void formatSummaryForShownotes_htmlNewlines() throws Exception {
        String formatted = invokeFormatSummary("<p>Line one\nLine two</p>");
        assertEquals("<p>Line one<br />Line two</p>", formatted);
    }

    private static String invokeFormatSummary(String summary) throws Exception {
        Method method = PodcastSegmentsFormatter.class.getDeclaredMethod("formatSummaryForShownotes", String.class);
        method.setAccessible(true);
        return (String) method.invoke(null, summary);
    }
}
