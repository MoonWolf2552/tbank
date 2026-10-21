package academy.backend.market_pulse.exercise;

/**
 * Сверка эталонного решения ({@link StreamExercisesReference}) — должен быть зелёным всегда.
 * Запускается семинаристом перед занятием, чтобы убедиться, что формулировки упражнений и
 * ожидаемые результаты в {@link StreamExercisesContractTest} верны, прежде чем раздавать
 * {@link StreamExercisesSkeleton} студентам.
 */
class StreamExercisesReferenceTest extends StreamExercisesContractTest {

    @Override
    protected StreamExercises createExercises() {
        return new StreamExercisesReference();
    }
}
