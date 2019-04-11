/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.quickstep;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView.Adapter;

import com.android.launcher3.R;
import com.android.quickstep.views.TaskItemView;
import com.android.systemui.shared.recents.model.Task;

import java.util.List;
import java.util.Objects;

/**
 * Recycler view adapter that dynamically inflates and binds {@link TaskHolder} instances with the
 * appropriate {@link Task} from the recents task list.
 */
public final class TaskAdapter extends Adapter<TaskHolder> {

    public static final int CHANGE_EVENT_TYPE_EMPTY_TO_CONTENT = 0;

    private static final int MAX_TASKS_TO_DISPLAY = 6;
    private static final String TAG = "TaskAdapter";
    private final TaskListLoader mLoader;
    private TaskActionController mTaskActionController;
    private boolean mIsShowingLoadingUi;

    public TaskAdapter(@NonNull TaskListLoader loader) {
        mLoader = loader;
    }

    public void setActionController(TaskActionController taskActionController) {
        mTaskActionController = taskActionController;
    }

    /**
     * Sets all positions in the task adapter to loading views, binding new views if necessary.
     * This changes the task adapter's view of the data, so the appropriate notify events should be
     * called in addition to this method to reflect the changes.
     *
     * @param isShowingLoadingUi true to bind loading task views to all positions, false to return
     *                           to the real data
     */
    public void setIsShowingLoadingUi(boolean isShowingLoadingUi) {
        mIsShowingLoadingUi = isShowingLoadingUi;
    }

    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TaskItemView itemView = (TaskItemView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item_view, parent, false);
        TaskHolder holder = new TaskHolder(itemView);
        itemView.setOnClickListener(view -> mTaskActionController.launchTask(holder));
        return holder;
    }

    @Override
    public void onBindViewHolder(TaskHolder holder, int position) {
        onBindViewHolderInternal(holder, position, false /* willAnimate */);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskHolder holder, int position,
            @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
            return;
        }
        int changeType = (int) payloads.get(0);
        if (changeType == CHANGE_EVENT_TYPE_EMPTY_TO_CONTENT) {
            // Bind in preparation for animation
            onBindViewHolderInternal(holder, position, true /* willAnimate */);
        } else {
            throw new IllegalArgumentException("Payload content is not a valid change event type: "
                    + changeType);
        }
    }

    private void onBindViewHolderInternal(@NonNull TaskHolder holder, int position,
            boolean willAnimate) {
        if (mIsShowingLoadingUi) {
            holder.bindEmptyUi();
            return;
        }
        List<Task> tasks = mLoader.getCurrentTaskList();
        if (position >= tasks.size()) {
            // Task list has updated.
            return;
        }
        Task task = tasks.get(position);
        holder.bindTask(task, willAnimate /* willAnimate */);
        mLoader.loadTaskIconAndLabel(task, () -> {
            // Ensure holder still has the same task.
            if (Objects.equals(task, holder.getTask())) {
                holder.getTaskItemView().setIcon(task.icon);
                holder.getTaskItemView().setLabel(task.titleDescription);
            }
        });
        mLoader.loadTaskThumbnail(task, () -> {
            if (Objects.equals(task, holder.getTask())) {
                holder.getTaskItemView().setThumbnail(task.thumbnail.thumbnail);
            }
        });
    }

    @Override
    public int getItemCount() {
        if (mIsShowingLoadingUi) {
            // Show loading version of all items.
            return MAX_TASKS_TO_DISPLAY;
        } else {
            return Math.min(mLoader.getCurrentTaskList().size(), MAX_TASKS_TO_DISPLAY);
        }
    }
}
