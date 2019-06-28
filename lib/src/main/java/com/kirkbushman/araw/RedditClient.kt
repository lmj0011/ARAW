package com.kirkbushman.araw

import com.kirkbushman.araw.fetcher.CommentsFetcher
import com.kirkbushman.araw.fetcher.ContributionsFetcher
import com.kirkbushman.araw.fetcher.Fetcher
import com.kirkbushman.araw.fetcher.InboxFetcher
import com.kirkbushman.araw.fetcher.SubmissionsFetcher
import com.kirkbushman.araw.fetcher.SubredditsFetcher
import com.kirkbushman.araw.models.Account
import com.kirkbushman.araw.models.Comment
import com.kirkbushman.araw.models.Me
import com.kirkbushman.araw.models.Message
import com.kirkbushman.araw.models.Redditor
import com.kirkbushman.araw.models.Submission
import com.kirkbushman.araw.models.Subreddit
import com.kirkbushman.araw.models.SubredditRule
import com.kirkbushman.araw.models.Trophy
import com.kirkbushman.araw.models.WikiPage
import com.kirkbushman.araw.models.general.ContributionSorting
import com.kirkbushman.araw.models.general.SubmissionSorting
import com.kirkbushman.araw.models.general.TimePeriod
import com.kirkbushman.araw.models.general.Vote
import com.kirkbushman.araw.models.mixins.Contribution
import com.kirkbushman.araw.models.mixins.Votable
import com.kirkbushman.araw.utils.Utils.getRetrofit
import com.kirkbushman.auth.models.TokenBearer

class RedditClient(private val bearer: TokenBearer, logging: Boolean) {

    private val retrofit = getRetrofit(logging)
    private val api = retrofit.create(RedditApi::class.java)

    val messages by lazy { GeneralMessagesHandler(api, ::getHeaderMap) }
    val account by lazy { GeneralAccountHandler(api, ::getHeaderMap) }
    val selfAccount by lazy { SelfAccountHandler(api, { me() ?: throw IllegalStateException("Could not found logged user") }, ::getHeaderMap) }
    val contributions by lazy { GeneralContributionHandler(api, ::getHeaderMap) }
    val commonSubreddits by lazy { CommonSubredditsHandler(api, ::getHeaderMap) }

    fun me(): Me? {

        val authMap = getHeaderMap()
        val req = api.me(header = authMap)
        val res = req.execute()

        if (!res.isSuccessful) {
            return null
        }

        return res.body()
    }

    fun user(username: String): Redditor? {

        val authMap = getHeaderMap()
        val req = api.user(username = username, header = authMap)
        val res = req.execute()

        if (!res.isSuccessful) {
            return null
        }

        return res.body()?.data
    }

    fun subreddit(subreddit: String): Subreddit? {

        val authMap = getHeaderMap()
        val req = api.subreddit(subreddit = subreddit, header = authMap)
        val res = req.execute()

        if (!res.isSuccessful) {
            return null
        }

        return res.body()?.data
    }

    fun submission(submissionId: String): Submission? {

        val authMap = getHeaderMap()
        val req = api.submission(submissionId = "t3_$submissionId", header = authMap)
        val res = req.execute()

        if (!res.isSuccessful) {
            return null
        }

        // take the first in the listing
        return res.body()?.data?.children?.first()?.data
    }

    fun submissions(

        subreddit: String,
        limit: Int = Fetcher.DEFAULT_LIMIT,

        sorting: SubmissionSorting = SubmissionsFetcher.DEFAULT_SORTING,
        timePeriod: TimePeriod = SubmissionsFetcher.DEFAULT_TIMEPERIOD

    ): SubmissionsFetcher {
        return SubmissionsFetcher(

            api = api,
            subreddit = subreddit,
            limit = limit,
            sorting = sorting,
            timePeriod = timePeriod

        ) { getHeaderMap() }
    }

    fun comment(commentId: String): Comment? {

        val authMap = getHeaderMap()
        val req = api.comment(commentId = "t1_$commentId", header = authMap)
        val res = req.execute()

        if (!res.isSuccessful) {
            return null
        }

        return res.body()?.data?.children?.first()?.data
    }

    fun comments(

        submissionId: String,
        limit: Int = Fetcher.DEFAULT_LIMIT,
        depth: Int? = null

    ): CommentsFetcher {

        return CommentsFetcher(api, submissionId, limit = limit, depth = depth) { getHeaderMap() }
    }

    fun wiki(subreddit: String): WikiPage? {

        val authMap = getHeaderMap()
        val req = api.wiki(subreddit = subreddit, header = authMap)
        val res = req.execute()

        if (!res.isSuccessful) {
            return null
        }

        return res.body()?.data
    }

    fun rules(subreddit: String): Array<SubredditRule>? {

        val authMap = getHeaderMap()
        val req = api.rules(subreddit = subreddit, header = authMap)
        val res = req.execute()

        if (!res.isSuccessful) {
            return null
        }

        return res.body()?.rules
    }

    fun accountHandler(account: Account): AccountHandler {
        return AccountHandler(api, account, ::getHeaderMap)
    }

    fun contributionHandler(contribution: Contribution): ContributionHandler {
        return ContributionHandler(api, contribution, ::getHeaderMap)
    }

    private fun getHeaderMap(): HashMap<String, String> {
        return hashMapOf("Authorization" to "bearer ".plus(bearer.getRawAccessToken()))
    }

    class GeneralMessagesHandler(

        private val api: RedditApi,
        private inline val getHeaderMap: () -> HashMap<String, String>

    ) {

        fun inbox(limit: Int = Fetcher.DEFAULT_LIMIT): InboxFetcher {
            return InboxFetcher(api, "inbox", limit) { getHeaderMap() }
        }

        fun unread(limit: Int = Fetcher.DEFAULT_LIMIT): InboxFetcher {
            return InboxFetcher(api, "unread", limit) { getHeaderMap() }
        }

        fun messages(limit: Int = Fetcher.DEFAULT_LIMIT): InboxFetcher {
            return InboxFetcher(api, "messages", limit) { getHeaderMap() }
        }

        fun sent(limit: Int = Fetcher.DEFAULT_LIMIT): InboxFetcher {
            return InboxFetcher(api, "sent", limit) { getHeaderMap() }
        }

        fun commentsReplies(limit: Int = Fetcher.DEFAULT_LIMIT): InboxFetcher {
            return InboxFetcher(api, "comments", limit) { getHeaderMap() }
        }

        fun selfReplies(limit: Int = Fetcher.DEFAULT_LIMIT): InboxFetcher {
            return InboxFetcher(api, "selfreply", limit) { getHeaderMap() }
        }

        fun mentions(limit: Int = Fetcher.DEFAULT_LIMIT): InboxFetcher {
            return InboxFetcher(api, "mentions", limit) { getHeaderMap() }
        }

        fun markAsRead(read: Boolean, message: Message): Any? {
            return markAsRead(read, message.fullname)
        }

        fun markAsRead(read: Boolean, fullname: String): Any? {

            val authMap = getHeaderMap()
            val req = if (read)
                api.readMessage(id = fullname, header = authMap)
            else
                api.unreadMessage(id = fullname, header = authMap)

            val res = req.execute()

            if (!res.isSuccessful) {
                return null
            }

            return res.body()
        }
    }

    class GeneralAccountHandler(

        private val api: RedditApi,
        private inline val getHeaderMap: () -> HashMap<String, String>

    ) {

        fun overview(

            username: String,
            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = username,
                where = "",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun submitted(

            username: String,

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = username,
                where = "submitted",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun comments(

            username: String,

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = username,
                where = "comments",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun gilded(

            username: String,

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = username,
                where = "gilded",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun trophies(username: String): List<Trophy>? {

            val authMap = getHeaderMap()
            val req = api.userTrophies(username = username, header = authMap)
            val res = req.execute()

            if (!res.isSuccessful) {
                return null
            }

            return res.body()?.data?.trophies?.map { it.data }?.toList()
        }
    }

    class AccountHandler(

        api: RedditApi,
        private val user: Account,
        getHeaderMap: () -> HashMap<String, String>

    ) {

        private val accountHandler = GeneralAccountHandler(api, getHeaderMap)

        fun overview(limit: Int = Fetcher.DEFAULT_LIMIT): ContributionsFetcher {
            return accountHandler.overview(user.name, limit)
        }

        fun submitted(limit: Int = Fetcher.DEFAULT_LIMIT): ContributionsFetcher {
            return accountHandler.submitted(user.name, limit)
        }

        fun comments(limit: Int = Fetcher.DEFAULT_LIMIT): ContributionsFetcher {
            return accountHandler.comments(user.name, limit)
        }

        fun gilded(limit: Int = Fetcher.DEFAULT_LIMIT): ContributionsFetcher {
            return accountHandler.gilded(user.name, limit)
        }

        fun trophies(): List<Trophy>? {
            return accountHandler.trophies(user.name)
        }
    }

    class SelfAccountHandler(

        private val api: RedditApi,
        private inline val getSelfAccount: () -> Me,
        private inline val getHeaderMap: () -> HashMap<String, String>

    ) {

        private var currentUser: String? = null

        fun overview(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun submitted(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "submitted",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun comments(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "comments",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun saved(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "saved",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun hidden(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "hidden",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun upvoted(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "upvoted",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun downvoted(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "downvoted",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun gilded(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: ContributionSorting = ContributionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = ContributionsFetcher.DEFAULT_TIMEPERIOD

        ): ContributionsFetcher {

            return ContributionsFetcher(

                api = api,
                username = getUserCached(),
                where = "gilded",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun subscribedSubreddits(limit: Int = Fetcher.DEFAULT_LIMIT): SubredditsFetcher {
            return SubredditsFetcher(api, "subscriber", limit) { getHeaderMap() }
        }

        fun trophies(): List<Trophy>? {

            val authMap = getHeaderMap()
            val req = api.selfUserTrophies(header = authMap)
            val res = req.execute()

            if (!res.isSuccessful) {
                return null
            }

            return res.body()?.data?.trophies?.map { it.data }?.toList()
        }

        private fun getUserCached(): String {

            return if (currentUser != null) {
                currentUser!!
            } else {
                currentUser = getSelfAccount().name
                currentUser!!
            }
        }
    }

    class GeneralContributionHandler(

        private val api: RedditApi,
        private inline val getHeaderMap: () -> HashMap<String, String>

    ) {

        fun vote(vote: Vote, votable: Votable): Any? {
            return vote(vote, votable.fullname)
        }

        fun vote(vote: Vote, fullname: String): Any? {

            val authMap = getHeaderMap()
            val req = api.vote(id = fullname, dir = vote.dir, header = authMap)
            val res = req.execute()

            if (!res.isSuccessful) {
                return null
            }

            return res.body()
        }

        fun save(save: Boolean, contribution: Contribution): Any? {
            return save(save, contribution.fullname)
        }

        fun save(save: Boolean, fullname: String): Any? {

            val authMap = getHeaderMap()
            val req = if (save)
                api.save(id = fullname, header = authMap)
            else
                api.unsave(id = fullname, header = authMap)
            val res = req.execute()

            if (!res.isSuccessful) {
                return null
            }

            return res.body()
        }
    }

    class ContributionHandler(

        api: RedditApi,
        private val contribution: Contribution,
        getHeaderMap: () -> HashMap<String, String>

    ) {

        private val contributionsHandler = GeneralContributionHandler(api, getHeaderMap)

        fun vote(vote: Vote): Any? {
            return contributionsHandler.vote(vote, contribution as Votable)
        }

        fun save(save: Boolean): Any? {
            return contributionsHandler.save(save, contribution)
        }
    }

    class CommonSubredditsHandler(

        private val api: RedditApi,
        private inline val getHeaderMap: () -> HashMap<String, String>

    ) {

        fun frontpage(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: SubmissionSorting = SubmissionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = SubmissionsFetcher.DEFAULT_TIMEPERIOD

        ): SubmissionsFetcher {

            return SubmissionsFetcher(

                api = api,
                subreddit = "",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun all(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: SubmissionSorting = SubmissionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = SubmissionsFetcher.DEFAULT_TIMEPERIOD

        ): SubmissionsFetcher {

            return SubmissionsFetcher(

                api = api,
                subreddit = "all",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun popular(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: SubmissionSorting = SubmissionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = SubmissionsFetcher.DEFAULT_TIMEPERIOD

        ): SubmissionsFetcher {

            return SubmissionsFetcher(

                api = api,
                subreddit = "popular",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }

        fun friends(

            limit: Int = Fetcher.DEFAULT_LIMIT,

            sorting: SubmissionSorting = SubmissionsFetcher.DEFAULT_SORTING,
            timePeriod: TimePeriod = SubmissionsFetcher.DEFAULT_TIMEPERIOD

        ): SubmissionsFetcher {

            return SubmissionsFetcher(

                api = api,
                subreddit = "friends",
                limit = limit,
                sorting = sorting,
                timePeriod = timePeriod

            ) { getHeaderMap() }
        }
    }
}