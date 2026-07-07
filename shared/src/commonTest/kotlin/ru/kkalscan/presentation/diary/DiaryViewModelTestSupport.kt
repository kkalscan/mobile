package ru.kkalscan.presentation.diary

import kotlinx.coroutines.CoroutineScope
import ru.kkalscan.data.health.IHealthConnectReader
import ru.kkalscan.data.health.NoOpHealthConnectReader
import ru.kkalscan.data.repository.IDiaryRepository

fun createDiaryViewModelForTest(
    diaryRepository: IDiaryRepository,
    scope: CoroutineScope,
    healthConnect: IHealthConnectReader = NoOpHealthConnectReader(),
): DiaryViewModel = DiaryViewModel(
    diaryRepository = diaryRepository,
    healthConnect = healthConnect,
    scope = scope,
)
