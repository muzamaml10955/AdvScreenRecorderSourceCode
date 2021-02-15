package com.coolapps.screenrecorder.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.coolapps.screenrecorder.Const;
import com.coolapps.screenrecorder.R;
import com.coolapps.screenrecorder.encoder.Mp4toGIFConverter;
import com.coolapps.screenrecorder.ui.EditVideoActivity;
import com.coolapps.screenrecorder.ui.VideosListFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;


public class VideoRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_SECTION = 0;

    private static final int VIEW_ITEM = 1;

    private VideosListFragment videosListFragment;

    private ArrayList<Video> videos;

    private Context context;

    private boolean isMultiSelect = false;

    private int count = 0;

    private ActionMode mActionMode;

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.video_list_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Build an arraylist of selected item positions when an menu item is clicked
            switch (item.getItemId()) {
                case R.id.delete:
                    ArrayList<Video> deleteFiles = new ArrayList<>();
                    for (Video video : videos) {
                        if (video.isSelected()) {
                            deleteFiles.add(video);
                        }
                    }
                    if (!deleteFiles.isEmpty())
                        confirmDelete(deleteFiles);
                    mActionMode.finish();
                    break;
                case R.id.share:
                    ArrayList<Integer> positions = new ArrayList<>();
                    for (Video video : videos) {
                        if (video.isSelected()) {
                            positions.add(videos.indexOf(video));
                        }
                    }
                    if (!positions.isEmpty())
                        shareVideos(positions);
                    mActionMode.finish();
                    break;
                case R.id.select_all:
                    for (Video video : videos)
                        video.setSelected(true);
                    mActionMode.setTitle("" + videos.size());
                    notifyDataSetChanged();
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            for (Video video :
                    videos) {
                video.setSelected(false);
            }
            isMultiSelect = false;
            notifyDataSetChanged();
        }
    };

    public VideoRecyclerAdapter(Context context, ArrayList<Video> android, VideosListFragment videosListFragment) {
        this.videos = android;
        this.context = context;
        this.videosListFragment = videosListFragment;
    }

    @Override
    public int getItemViewType(int position) {
        return isSection(position) ? VIEW_SECTION : VIEW_ITEM;
    }

    public boolean isSection(int position) {
        return videos.get(position).isSection();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_SECTION:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_video_section, viewGroup, false);
                return new SectionViewHolder(view);
            case VIEW_ITEM:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_video, viewGroup, false);
                return new ItemViewHolder(view);
            default:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_video, viewGroup, false);
                return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final Video video = videos.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_ITEM:
                final ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
                //Set video file name
                itemViewHolder.tv_fileName.setText(video.getFileName());
                //If thumbnail has failed for some reason, set empty image resource to imageview
                if (videos.get(position).getThumbnail() != null) {
                    itemViewHolder.iv_thumbnail.setImageBitmap(video.getThumbnail());
                } else {
                    itemViewHolder.iv_thumbnail.setImageResource(0);
                    Log.d(Const.TAG, "thumbnail error");
                }

                // Hide the play image over thumbnail and overflow menu if multiselect enabled
                if (isMultiSelect) {
                    itemViewHolder.iv_play.setVisibility(View.INVISIBLE);
                    itemViewHolder.overflow.setVisibility(View.INVISIBLE);
                } else {
                    itemViewHolder.iv_play.setVisibility(View.VISIBLE);
                    itemViewHolder.overflow.setVisibility(View.VISIBLE);
                }

                if (video.isSelected()) {
                    itemViewHolder.selectableFrame.setForeground(new ColorDrawable(ContextCompat.getColor(context, R.color.multiSelectColor)));
                } else {
                    itemViewHolder.selectableFrame.setForeground(new ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)));
                }

                itemViewHolder.overflow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PopupMenu popupMenu = new PopupMenu(context, view);
                        popupMenu.inflate(R.menu.popupmenu);
                        popupMenu.show();
                        popupMenu.getMenu().getItem(3).setEnabled(PreferenceManager.getDefaultSharedPreferences(context)
                                .getBoolean(
                                        context.getString(R.string.preference_save_gif_key), false));
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.share:
                                        shareVideo(itemViewHolder.getAdapterPosition());
                                        break;
                                    case R.id.delete:
                                        confirmDelete(holder.getAdapterPosition());
                                        break;
                                    case R.id.edit:
                                        Toast.makeText(context, "Edit video for " + itemViewHolder.getAdapterPosition(), Toast.LENGTH_SHORT).show();

                                        Intent editIntent = new Intent(context, EditVideoActivity.class);
                                        editIntent.putExtra(Const.VIDEO_EDIT_URI_KEY,
                                                Uri.fromFile(video.getFile()).toString());
                                        Log.d(Const.TAG, "Uri: " + Uri.fromFile(video.getFile()));
                                        videosListFragment.startActivityForResult(editIntent, Const.VIDEO_EDIT_REQUEST_CODE);
                                        break;
                                    case R.id.savegif:
                                        Mp4toGIFConverter gif = new Mp4toGIFConverter(context);
                                        gif.setVideoUri(Uri.fromFile(video.getFile()));
                                        gif.convertToGif();
                                }
                                return true;
                            }
                        });
                    }
                });

                itemViewHolder.videoCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        if (isMultiSelect) {

                            if (video.isSelected())
                                count--;
                            else
                                count++;

                            video.setSelected(!video.isSelected());
                            notifyDataSetChanged();
                            mActionMode.setTitle("" + count);

                            if (count == 0)
                                setMultiSelect(false);
                            return;
                        }

                        File videoFile = video.getFile();
                        Log.d("Videos List", "video position clicked: " + itemViewHolder.getAdapterPosition());

                        Uri fileUri = FileProvider.getUriForFile(
                                context, context.getPackageName() +
                                        ".provider",
                                videoFile
                        );
                        Log.d(Const.TAG, fileUri.toString());
                        Intent openVideoIntent = new Intent();
                        openVideoIntent.setAction(Intent.ACTION_VIEW)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .setDataAndType(
                                        fileUri,
                                        context.getContentResolver().getType(fileUri));
                        context.startActivity(openVideoIntent);
                    }
                });

                itemViewHolder.videoCard.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (!isMultiSelect) {
                            setMultiSelect(true);
                            video.setSelected(true);
                            count++;
                            mActionMode.setTitle("" + count);
                            notifyDataSetChanged();
                        }
                        return true;
                    }
                });

                break;
            case VIEW_SECTION:
                SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
                sectionViewHolder.section.setText(generateSectionTitle(video.getLastModified()));
                break;
        }
    }

    private void setMultiSelect(boolean isMultiSelect) {
        if (isMultiSelect) {
            this.isMultiSelect = true;
            count = 0;
            mActionMode = ((AppCompatActivity) videosListFragment.getActivity()).startSupportActionMode(mActionModeCallback);
        } else {
            this.isMultiSelect = false;
            mActionMode.finish();
        }
    }

    private void shareVideo(int position) {
        Uri fileUri = FileProvider.getUriForFile(
                context, context.getPackageName() +
                        ".provider",
                videos.get(position).getFile()
        );

        Intent Shareintent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("video/*")
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, fileUri);
        context.startActivity(Intent.createChooser(Shareintent,
                context.getString(R.string.share_intent_notification_title)));
    }

    private void shareVideos(ArrayList<Integer> positions) {
        ArrayList<Uri> videoList = new ArrayList<>();
        for (int position : positions) {
            videoList.add(FileProvider.getUriForFile(
                    context, context.getPackageName() +
                            ".provider",
                    videos.get(position).getFile()
            ));
        }
        Intent Shareintent = new Intent()
                .setAction(Intent.ACTION_SEND_MULTIPLE)
                .setType("video/*")
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, videoList);
        context.startActivity(Intent.createChooser(Shareintent,
                context.getString(R.string.share_intent_notification_title)));
    }

    private void deleteVideo(int position) {
        Log.d("Videos List", "delete position clicked: " + position);
        File file = new File(videos.get(position).getFile().getPath());
        if (file.delete()) {
            Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show();
            videos.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, videos.size());
        }
    }

    private void deleteVideos(ArrayList<Video> deleteVideos) {
        for (Video video : deleteVideos) {
            if (!video.isSection() && video.getFile().delete()) {
                notifyItemRemoved(videos.indexOf(video));
                videos.remove(video);
            }
        }
        notifyDataSetChanged();
    }

    private void confirmDelete(final int position) {
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getQuantityString(R.plurals.delete_alert_title, 1))
                .setMessage(context.getResources().getQuantityString(R.plurals.delete_alert_message, 1))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteVideo(position);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .show();
    }

    private void confirmDelete(final ArrayList<Video> deleteVideos) {
        int count = deleteVideos.size();
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getQuantityString(R.plurals.delete_alert_title, count))
                .setMessage(context.getResources().getQuantityString(R.plurals.delete_alert_message,
                        count, count))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deleteVideos(deleteVideos);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .show();
    }

    private String generateSectionTitle(Date date) {
        Calendar sDate = toCalendar(new Date().getTime());
        Calendar eDate = toCalendar(date.getTime());

        // Get the represented date in milliseconds
        long milis1 = sDate.getTimeInMillis();
        long milis2 = eDate.getTimeInMillis();

        // Calculate difference in milliseconds
        int dayDiff = (int) Math.abs((milis2 - milis1) / (24 * 60 * 60 * 1000));

        int yearDiff = sDate.get(Calendar.YEAR) - eDate.get(Calendar.YEAR);
        Log.d("ScreenRecorder", "yeardiff: " + yearDiff);

        if (yearDiff == 0) {
            switch (dayDiff) {
                case 0:
                    return "Today";
                case 1:
                    return "Yesterday";
                default:
                    SimpleDateFormat format = new SimpleDateFormat("EEEE, dd MMM", Locale.getDefault());
                    return format.format(date);
            }
        } else {
            SimpleDateFormat format = new SimpleDateFormat("EEEE, dd MMM YYYY", Locale.getDefault());
            return format.format(date);
        }
    }

    private Calendar toCalendar(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    private final class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_fileName;
        private ImageView iv_thumbnail;
        private RelativeLayout videoCard;
        private FrameLayout selectableFrame;
        private ImageButton overflow;
        private ImageView iv_play;

        ItemViewHolder(View view) {
            super(view);
            tv_fileName = view.findViewById(R.id.fileName);
            iv_thumbnail = view.findViewById(R.id.thumbnail);
            iv_thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            videoCard = view.findViewById(R.id.videoCard);
            overflow = view.findViewById(R.id.ic_overflow);
            selectableFrame = view.findViewById(R.id.selectableFrame);
            iv_play = view.findViewById(R.id.play_iv);
        }
    }

    private final class SectionViewHolder extends RecyclerView.ViewHolder {
        private TextView section;

        SectionViewHolder(View view) {
            super(view);
            section = view.findViewById(R.id.sectionID);
        }
    }
}
