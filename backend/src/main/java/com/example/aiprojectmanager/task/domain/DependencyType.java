package com.example.aiprojectmanager.task.domain;

/**
 * Defines how a successor task's schedule relates to its predecessor.
 *
 * <ul>
 *   <li>FINISH_TO_START  (FS) — successor can start only after predecessor finishes (most common)</li>
 *   <li>START_TO_START   (SS) — successor can start only after predecessor starts</li>
 *   <li>FINISH_TO_FINISH (FF) — successor can finish only after predecessor finishes</li>
 *   <li>START_TO_FINISH  (SF) — successor can finish only after predecessor starts</li>
 * </ul>
 */
public enum DependencyType {
    FINISH_TO_START,
    START_TO_START,
    FINISH_TO_FINISH,
    START_TO_FINISH
}
