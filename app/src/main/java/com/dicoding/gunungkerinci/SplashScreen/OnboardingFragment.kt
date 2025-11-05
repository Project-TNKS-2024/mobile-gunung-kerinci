package com.dicoding.gunungkerinci.SplashScreen

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dicoding.gunungkerinci.R

class OnboardingFragment : Fragment() {

    companion object{
        private const val ARG_TITLE = "title"
        private const val ARG_DESC = "desc"
        private const val ARG_IMAGE = "image"

        fun newInstance(title: String, desc: String, imageRes: Int): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putString(ARG_TITLE, title)
            args.putString(ARG_DESC, desc)
            args.putInt(ARG_IMAGE, imageRes)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)

        val titleText: TextView = view.findViewById(R.id.textBesar)
        val descText: TextView = view.findViewById(R.id.textDeskripsi)
        val image: ImageView = view.findViewById(R.id.imageSplash)

        arguments?.let {
            titleText.text = it.getString(ARG_TITLE)
            descText.text = it.getString(ARG_DESC)
            image.setImageResource(it.getInt(ARG_IMAGE))
        }

        return view
    }
}