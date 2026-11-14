package com.nospam.data.db

import androidx.room.TypeConverter
import com.nospam.data.model.RuleAction
import com.nospam.data.model.RuleType
import com.nospam.data.model.SpamCategory
import com.nospam.data.model.Verdict

/** Converte enum e liste di stringhe per la persistenza Room. */
class Converters {
    @TypeConverter fun verdictToString(v: Verdict) = v.name
    @TypeConverter fun stringToVerdict(s: String) = Verdict.valueOf(s)

    @TypeConverter fun categoryToString(c: SpamCategory) = c.name
    @TypeConverter fun stringToCategory(s: String) = SpamCategory.valueOf(s)

    @TypeConverter fun ruleTypeToString(t: RuleType) = t.name
    @TypeConverter fun stringToRuleType(s: String) = RuleType.valueOf(s)

    @TypeConverter fun ruleActionToString(a: RuleAction) = a.name
    @TypeConverter fun stringToRuleAction(s: String) = RuleAction.valueOf(s)

    @TypeConverter fun listToString(list: List<String>): String =
        list.joinToString("\u001F") // separatore unit-separator, improbabile nei testi
    @TypeConverter fun stringToList(s: String): List<String> =
        if (s.isEmpty()) emptyList() else s.split("\u001F")
}
