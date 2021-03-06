package com.kirkbushman.sampleapp.activities

import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.kirkbushman.araw.RedditClient
import com.kirkbushman.araw.models.Submission
import com.kirkbushman.sampleapp.R
import com.kirkbushman.sampleapp.activities.base.BaseSearchPrintActivity
import kotlinx.android.synthetic.main.activity_submission.*

class SubmissionActivity : BaseSearchPrintActivity<Submission>(R.layout.activity_submission) {

    companion object {

        fun start(context: Context) {

            val intent = Intent(context, SubmissionActivity::class.java)
            context.startActivity(intent)
        }
    }

    override val actionBar: Toolbar
        get() = toolbar

    override val bttnSearch: Button
        get() = bttn_search

    override val editSearch: EditText
        get() = edit_submission_id

    override val textPrint: TextView
        get() = submission_text

    override fun fetchItem(client: RedditClient?, query: String): Submission? {
        return client?.contributionsClient?.submission(query)
    }
}
